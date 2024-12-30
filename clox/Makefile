CC      = gcc
CFLAGS  = -Wall -Wextra -std=c99 -O2

TARGET  = clox

SRCS    = main.c chunk.c
OBJS    = $(SRCS:.c=.o)

.PHONY: default
default: help

.PHONY: all
all: $(TARGET)

$(TARGET): $(OBJS)
	@echo "[Linking] $@"
	$(CC) $(OBJS) -o $(TARGET)

%.o: %.c
	@echo "[Compiling] $<"
	$(CC) $(CFLAGS) -c $< -o $@

.PHONY: format
format:
	@echo "[Formatting source code with clang-format]"
	clang-format -i *.c *.h

.PHONY: run
run: $(TARGET)
	@echo "[Running] ./$(TARGET)"
	./$(TARGET)

.PHONY: run-clean
run-clean: rebuild
	@echo "[Running fresh build] ./$(TARGET)"
	./$(TARGET)

.PHONY: rebuild
rebuild: clean all

.PHONY: clean
clean:
	@echo "[Cleaning build files]"
	rm -f $(OBJS) $(TARGET)

.PHONY: help
help:
	@echo "Usage: make [TARGET]"
	@echo
	@echo "Targets:"
	@echo "  all        - Build the project ($(TARGET))"
	@echo "  format     - Format source code with clang-format"
	@echo "  run        - Run the compiled binary"
	@echo "  run-clean  - Clean & rebuild, then run"
	@echo "  rebuild    - Clean then rebuild the project"
	@echo "  clean      - Remove all build artifacts"
	@echo "  help       - Show this help"
