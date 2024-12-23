SOURCES := $(shell find com/craftinginterpreters/lox -name "*.java")

gen: build-tool
	java com.craftinginterpreters.tool.GenerateAst com/craftinginterpreters/lox
	google-java-format -i com/craftinginterpreters/lox/Expr.java
build-tool:
	javac com/craftinginterpreters/tool/GenerateAst.java
run: build
	java com.craftinginterpreters.lox.Lox
build: format
	javac com/craftinginterpreters/lox/Lox.java
format:
	google-java-format -i $(SOURCES)

.PHONY: build run format
