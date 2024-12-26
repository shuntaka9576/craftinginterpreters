SOURCES := $(shell find com/craftinginterpreters/lox -name "*.java")

gen: build-tool
	java com.craftinginterpreters.tool.GenerateAst com/craftinginterpreters/lox
	google-java-format -i com/craftinginterpreters/lox/Expr.java
	google-java-format -i com/craftinginterpreters/lox/Stmt.java
	@$(MAKE) format

build-tool:
	javac com/craftinginterpreters/tool/GenerateAst.java

run: build
	java com.craftinginterpreters.lox.Lox
	java com.craftinginterpreters.lox.AstPrinter

build: format
	javac com/craftinginterpreters/lox/Lox.java
	javac com/craftinginterpreters/lox/AstPrinter.java

format:
	google-java-format -i $(SOURCES)

.PHONY: build run format
