

```bash
3*4+24==23
```

```bash
make build;java com.craftinginterpreters.lox.Lox examples/01_8-1.lox
make build;java com.craftinginterpreters.lox.Lox examples/02_8-5.lox
```

## 11.1

```lox
var a = "global";
{
  fun showA() {
    print a;
  }

  showA();
  var a = "block";
  showA();
}
```

```出力結果
global
block
```

これは時間軸上、こう返却されるべき

```出力結果
global
global
```

10章のclosure実装(ものの数行)でこの挙動は変わる。

# 11.4 静的スコープの実現

前提 `resolveLocal` するのは、式として変数を参照したときとなる。

* `visitAssignExpr`
* `visitVariableExpr`

1. Resolverの処理
1.1. `Resolver.java` 2.でbを見つけたら、近いスコープからbを探す。外のスコープになるほど距離は長くなる
1.2. `Interpreter.locals` に ASTのExprオブジェクトとスコープ距離(`distance`)を登録する
2. Interpreterの処理
2.1. `visitVariableExpr` の `lookUpVariable` で ASTのExprオブジェクトをキーに項1.2.のスコープ距離を取得
2.2. 項2.1. のスコープ距離分、Enviromentの連結リストを剥がし、該当する静的スコープの環境を取得
2.3. 項目2.2.の環境から、トークン名(変数名など)をキーに値を取得


```lox
{
  var b = 32;
  print b; // b:0 -> bはこのブロック内にあるため0
  fun showB() {
    print b; // b:1 -> 外側のvar b = 32宣言をキャプチャ。1スコープ下にあるため1
  }

  {
    var b = 24;
    showB(); // show:1 -> showは1スコープ下にあるため1
    print b; // b:0 -> bはこのブロック内にあるため0
  }

  {
    showB(); // show:1 -> showは1スコープ下にあるため1
    print b; // b:1 -> bは1スコープ下にあるため1
  }

  {
    {
      showB(); // show:2 -> showは2スコープ下にあるため1
      print b; // b:2 -> bは2スコープ下にあるため1
    }
  }
}
```
