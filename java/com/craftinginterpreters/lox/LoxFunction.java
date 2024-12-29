package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  private final Environment closure;
  private final boolean isInitializer;

  // NOTE: 10.6. closure対応で引数拡張
  // LoxFunction(Stmt.Function declaration) {
  LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
    this.isInitializer = isInitializer;
    this.closure = closure;
    this.declaration = declaration;
  }

  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance);

    return new LoxFunction(declaration, environment, isInitializer);
    // NOTE: 12.7.1で置換
    // return new LoxFunction(declaration, environment);
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    // NOTE: 10.6. closure対応で引数拡張
    // Environment environment = new Environment(interpreter.globals);
    Environment environment = new Environment(closure);
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme, arguments.get(i)); // 環境に引数を入れている(!)
    }

    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
      if (isInitializer) return closure.getAt(0, "this");

      return returnValue.value; // 早期リターンの場合、コールスタックでバケツリレーする必要がるためtry catchで帯域脱出する
    }

    if (isInitializer) return closure.getAt(0, "this");

    // NOTE: 10.5で置換
    // interpreter.executeBlock(declaration.body, environment);
    return null;
  }
}

/*
```lox
fun makeCounter() {
  var i = 0;
  fun count() {
    i = i + 1;
    print i;
  }

  return count;
}

var counter = makeCounter();
counter();
counter();
```


# 変更前
```java:LoxFunction.java(変更前)
Environment environment = new Environment(interpreter.globals);

for (int i = 0; i < declaration.params.size(); i++) {
  environment.define(declaration.params.get(i).lexeme, arguments.get(i));
}

// 以降は変更なし
try {
  interpreter.executeBlock(declaration.body, environment); // この処理でvar iがenvironmentに入る
} catch (Return returnValue) {
  return returnValue.value;
}
```

executeBlockは、引数で指定された環境を元に文を実行する。
```java
  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;

    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }
```

しかし、executeBlockでcountの実行時にnew LoxFunctionするが、前述コードの通り、interpreter.globalsで初期化されてしまう。ここにはvar iの情報がない

```java:Interpreter.java(変更前)
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction function = new LoxFunction(stmt);
    environment.define(stmt.name.lexeme, function);
    return null;
  }
```

# 変更後

```java:LoxFunction.java(変更後)
private final Environment closure;
...
Environment environment = new Environment(closure);

for (int i = 0; i < declaration.params.size(); i++) {
  environment.define(declaration.params.get(i).lexeme, arguments.get(i));
}

// 以降は変更なし
try {
  interpreter.executeBlock(declaration.body, environment); // この処理でvar iがenvironmentに入る
} catch (Return returnValue) {
  return returnValue.value;
}
```

visitFunctionStmtの際に、executeBlockで初期化された環境でmakeCounterが初期化されるため、var iがcount関数でキャプチャできるようになる
```java:Interpreter.java(変更前)
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction function = new LoxFunction(stmt, environment);
    environment.define(stmt.name.lexeme, function);
    return null;
  }
```


通して、closureが親(makeCounter)で宣言された変数群を封じ込めて維持するので、閉包(closure)と命名できる
関数毎にclosureという保存領域(environment)を持つことで、count()を繰り返し読んでiがインクリメントできるような処理が可能になる。
*/
