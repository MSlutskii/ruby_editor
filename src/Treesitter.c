#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <tree_sitter/api.h>

#include "Treesitter.h"

TSLanguage* tree_sitter_ruby();

JNIEXPORT jlong JNICALL Java_Treesitter_parserNew(JNIEnv* env, jclass self) {
  TSParser* parser = ts_parser_new();
  ts_parser_set_language(parser, tree_sitter_ruby());
  return (jlong)parser;
}

JNIEXPORT void JNICALL Java_Treesitter_parserDelete(JNIEnv* env, jclass self,
                                                    jlong parser) {
  ts_parser_delete((TSParser*)parser);
}

JNIEXPORT jlong JNICALL Java_Treesitter_queryNew(JNIEnv* env, jclass self,
                                                 jbyteArray query_bytes,
                                                 jint query_len) {
  uint32_t error_offset = 0;
  TSQueryError error_type = TSQueryErrorNone;
  jbyte* query_str = (*env)->GetByteArrayElements(env, query_bytes, NULL);

  // TODO: process possible query errors. Case: error_type != TSQueryErrorNone.
  TSQuery* query = ts_query_new(tree_sitter_ruby(), (const char*)(query_str),
                                query_len, &error_offset, &error_type);
  (*env)->ReleaseByteArrayElements(env, query_bytes, query_str, JNI_ABORT);
  return (jlong)query;
}

JNIEXPORT void JNICALL Java_Treesitter_queryDelete(JNIEnv* env, jclass self,
                                                   jlong query) {
  ts_query_delete((TSQuery*)query);
}

JNIEXPORT jlong JNICALL Java_Treesitter_queryCursorNew(JNIEnv* env,
                                                       jclass self) {
  return (jlong)(ts_query_cursor_new());
}

JNIEXPORT void JNICALL Java_Treesitter_queryCursorDelete(JNIEnv* env,
                                                         jclass self,
                                                         jlong cursor) {
  ts_query_cursor_delete((TSQueryCursor*)cursor);
}

JNIEXPORT jintArray JNICALL Java_Treesitter_highlight(JNIEnv* env, jclass self,
                                                      jbyteArray source_bytes,
                                                      jint source_len,
                                                      jlong parser, jlong query,
                                                      jlong cursor) {
  jbyte* source = (*env)->GetByteArrayElements(env, source_bytes, NULL);

  TSTree* tree = ts_parser_parse_string_encoding(
      (TSParser*)parser, NULL /*first call*/, (const char*)(source), source_len,
      TSInputEncodingUTF8);
  TSNode root_node = ts_tree_root_node(tree);
  ts_query_cursor_exec((TSQueryCursor*)cursor, (TSQuery*)query, root_node);

  TSQueryMatch match;
  TSNode node;

  // Stores triplets: (node_start_byte,node_end_byte, capture_index).
  uint32_t* highlights = NULL;
  uint64_t cur_size = 0, max_size = 0;
  uint32_t start, end;
  uint32_t last_node_start = -1, last_node_end = -1;
  while (ts_query_cursor_next_match((TSQueryCursor*)cursor, &match)) {
    for (int capture_id = 0; capture_id < match.capture_count; capture_id++) {
      node = match.captures[capture_id].node;
      start = ts_node_start_byte(node);
      end = ts_node_end_byte(node);
      if (start == last_node_start && end == last_node_end) {
        continue;
      }
      last_node_start = start;
      last_node_end = end;
      uint32_t capture_index = match.captures[capture_id].index;

      // Reallocate memory similar to std::vector in C++.
      if (max_size < cur_size + 3) {
        max_size = 2 * max_size + 3;
        // 4 bytes is a size of uint32_t
        // TODO: check if realloc manages to allocate memory.
        highlights = realloc(highlights, max_size * 4);
      }
      highlights[(cur_size++)] = start;
      highlights[(cur_size++)] = end;
      highlights[(cur_size++)] = capture_index;
    }
  }

  jint fill[cur_size];
  for (int i = 0; i < cur_size; i++) {
    fill[i] = highlights[i];
  }
  jintArray result;
  result = (*env)->NewIntArray(env, cur_size);
  (*env)->SetIntArrayRegion(env, result, 0, cur_size, fill);

  free(highlights);
  ts_tree_delete(tree);
  return result;
}
