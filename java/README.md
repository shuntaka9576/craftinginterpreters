

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
