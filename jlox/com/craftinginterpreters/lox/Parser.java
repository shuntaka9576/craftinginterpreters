package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Parser {
  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  /* NOTE: 8.1.2で置換
  Expr parse() {
    try {
      return expression();
    } catch (ParseError error) {
      return null;
    }
  }
  */

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
      // NOTE: 8.2.2. で上記行のものに置換
      // statements.add(statement());
    }

    return statements;
  }

  private Expr expression() {
    return assignment();
  }

  private Expr assignment() {
    // NOTE: 9.3. で置換
    // Expr expr = equality();
    Expr expr = or();

    if (match(EQUAL)) {
      Token equals = previous(); // EQUALであることは明示的だが、matchで消費して次に進んでいるため位置情報取得ため取り出している
      Expr value = assignment(); // a = b = cのようなケースを想定

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable) expr).name;

        return new Expr.Assign(name, value);
      } else if (expr instanceof Expr.Get) {
        Expr.Get get = (Expr.Get) expr;

        return new Expr.Set(get.object, get.name, value);
      }

      error(equals, "Invalid assignment target.");
    }

    return expr;
  }

  private Expr or() {
    Expr expr = and();

    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr and() {
    Expr expr = equality();

    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Stmt declaration() {
    try {
      if (match(FUN)) return function("function");
      if (match(VAR)) return varDeclaration(); // VARが見つかり、match内でcurrent++
      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt classDeclaration() {
    Token name = consume(IDENTIFIER, "Expect class name.");

    Expr.Variable superclass = null;
    if (match(LESS)) {
      consume(IDENTIFIER, "Expect superclass name.");
      superclass = new Expr.Variable(previous());
    }

    consume(LEFT_BRACE, "Expect '{' before class body.");

    List<Stmt.Function> methods = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      methods.add(function("method"));
    }

    consume(RIGHT_BRACE, "Expect '}' after class body.");

    // NOTE: 13.1.で置換
    return new Stmt.Class(name, superclass, methods);
    // return new Stmt.Class(name, methods);
  }

  private Stmt statement() {
    if (match(CLASS)) return classDeclaration();
    if (match(IF)) return ifStatement();
    if (match(FOR)) return forStatment();
    if (match(PRINT)) return printStatement();
    if (match(RETURN)) return returnStatement();
    if (match(WHILE)) return whileStatement();
    if (match(LEFT_BRACE)) return new Stmt.Block(block());

    return expressionStatement();
  }

  // NOTE:
  // --- EBNF ---
  // "if" "(" expression ")" statment
  // ("else" statement )? ;
  // 「ぶら下がり else」問題が起きないよう、パーサの段階でそもそも曖昧さが生じないルールになってる
  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'if'."); // if の次は(のため
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if condition.");

    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  // NOTE: forは糖衣構文のため、既存のASTを応用して実現する
  private Stmt forStatment() {
    consume(LEFT_PAREN, "Expect '(' after 'for'."); // if の次は(のため

    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    } else if (match(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement(); // (1) stament
    }

    Expr condition = null;
    if (!check(SEMICOLON)) {
      condition = expression();
    }
    consume(SEMICOLON, "Expr ';' after loop condition.");

    Expr increment = null;
    if (!check(RIGHT_PAREN)) {
      increment = expression();
    }
    consume(RIGHT_PAREN, "Expect ')' after for clauses.");

    // NOTE: 以降の処理は、bodyを再代入を繰り返すため注意
    Stmt body = statement(); // (2) 1と同じstatementだが、式がネストする(if () {for ()})の可能性を考慮する必要がある。

    if (increment != null) {
      // NOTE: forブロックの中で、bodyとincrementの両方を実行したと解釈する
      // var i = 0
      // while (i < 10) {
      //   print i;
      //   i = i + 1; // <-- この部分
      // }
      body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
    }

    if (condition == null) {
      condition = new Expr.Literal(true);
    }

    body = new Stmt.While(condition, body); // <- (注意!) ここでwhileのASTとしてbodyが代入される

    if (initializer != null) {
      // NOTE: forブロックの中で1回だけ初期化を実行
      // var i = 0 // <-- この部分
      // while (i < 10) {
      //   print i;
      //   i = i + 1;
      // }
      body =
          new Stmt.Block(
              Arrays.asList(initializer, body)); // Blockの中でinitializerのあとにStmt.WhileのASTが追加される
    }

    return body;
  }

  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  private Stmt returnStatement() {
    Token keyword = previous();
    Expr value = null;
    if (!check(SEMICOLON)) {
      value = expression();
    }

    consume(SEMICOLON, "Expect ';' after return value.");
    return new Stmt.Return(keyword, value);
  }

  private Stmt varDeclaration() {
    // declarationのmatchで今はvar a=24だとするとa部分を指す。ここを消費しつつ、current++
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(EQUAL)) { // = を消費
      initializer = expression(); // initializerに右辺部を格納
    }

    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after condition.");
    Stmt body = statement();

    return new Stmt.While(condition, body);
  }

  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }

  // MEMO: finishCallとの違い(?)
  // おそらく定義する話(function)と呼び出す話(finishCall)
  private Stmt.Function function(String kind) {
    Token name = consume(IDENTIFIER, "Expect" + kind + " name.");
    consume(LEFT_PAREN, "Expect '(' after" + kind + " name.");
    List<Token> parameters = new ArrayList<>();

    if (!check(RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments.");
        }

        parameters.add(consume(IDENTIFIER, "Expect parameter name."));
      } while (match(COMMA));
    }
    consume(RIGHT_PAREN, "Expect ')' after parameters.");

    consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
    List<Stmt> body = block();

    return new Stmt.Function(name, parameters, body);
  }

  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expedct '}' after block.");
    return statements;
  }

  private Expr equality() {
    Expr expr = comparison();
    // matchで指定したタイプ群と一致しない場合、falseを返却。whileループ終了。
    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right); // exprが左に結合していく
    }

    return expr;
  }

  private Expr comparison() {
    Expr expr = term();
    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right); // exprが左に結合していく
    }

    return expr;
  }

  private Expr term() {
    Expr expr = factor();
    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right); // exprが左に結合していく
    }

    return expr;
  }

  private Expr factor() {
    Expr expr = unary();
    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right); // exprが左に結合していく
    }

    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return call();
    // NOTE: 10.1で置換
    // return primary();
  }

  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments.");
        }

        arguments.add(expression());
      } while (match(COMMA));
    }
    Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

    return new Expr.Call(callee, paren, arguments);
  }

  private Expr call() {
    Expr expr = primary();

    while (true) {
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else if (match(DOT)) {
        Token name = consume(IDENTIFIER, "Expect property name after '.'.");
        expr = new Expr.Get(expr, name);
      } else {
        break;
      }
    }

    return expr;
  }

  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(SUPER)) {
      Token keyword = previous();
      consume(DOT, "Expect ',' after 'super'.");
      Token method = consume(IDENTIFIER, "Expect superclass method name.");

      return new Expr.Super(keyword, method);
    }

    if (match(THIS)) return new Expr.This(previous());

    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();

    throw error(peek(), message);
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }

  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  private void synchronize() {
    advance();
    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FOR:
        case FUN:
        case IF:
        case PRINT:
        case RETURN:
        case VAR:
        case WHILE:
          return;
      }

      advance();
    }
  }
}
