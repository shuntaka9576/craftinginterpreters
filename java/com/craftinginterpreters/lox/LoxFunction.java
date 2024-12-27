package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;

  LoxFunction(Stmt.Function declaration) {
    this.declaration = declaration;
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    // 環境変数に引数の内容を追加している？実態はシステム的な環境変数とは呼べない気がする。
    Environment environment = new Environment(interpreter.globals);
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme, arguments.get(i));
    }

    interpreter.executeBlock(declaration.body, environment);
    return null;
  }
}
