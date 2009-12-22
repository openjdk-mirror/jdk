#ifndef SUN_AWT_UTIL_DEBUG_H
#define SUN_AWT_UTIL_DEBUG_H

#include <cstdio>
#include <app/Message.h>
#include <support/Debug.h>
#include "debug_util.h"

#define LOCK_LOOPER_FAILED() fprintf(stderr, "failed to lock the looper in %s\n", __PRETTY_FUNCTION__)

#define PRINT_PRETTY_FUNCTION() fprintf(stdout, "%s\n", __PRETTY_FUNCTION__)

#undef DEBUGGER
#define DEBUGGER() debugger(__PRETTY_FUNCTION__)

#define TODO() fprintf(stdout, "TODO: %s\n", __PRETTY_FUNCTION__)

#define BAD_MESSAGE() fprintf(stderr, "%s: poorly formed message\n", __PRETTY_FUNCTION__)

// prints "MESSAGE[<what>]" to FILE
void fprint_message(FILE * file, BMessage * message);

#endif // SUN_AWT_UTIL_DEBUG_H
