#include <stdio.h>

#include "chunk.h"
#include "memory.h"

void initChunk(Chunk *chunk) {
  chunk->count = 0;
  chunk->capacity = 0;
  chunk->code = NULL;
}

void writeChunk(Chunk *chunk, uint8_t byte) {
  if (chunk->capacity < chunk->count + 1) {
    int oldCpacity = chunk->capacity;
    chunk->capacity = GROW_CAPACITY(oldCpacity);
    chunk->code = GROW_ARRAY(uint8_t, chunk->code, oldCpacity, chunk->capacity);
  }

  chunk->code[chunk->count] = byte;
  chunk->count++;
}
