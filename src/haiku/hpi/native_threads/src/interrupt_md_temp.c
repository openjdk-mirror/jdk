#include "hpi_impl.h"

// temporary place holders, till I figure out what is happening
signal_handler_t sysSignal(int sig, signal_handler_t handler)
{
	signal_handler_t f;
	return f;
}
void sysRaise(int sig) {}
void sysSignalNotify(int sig) {}
int sysSignalWait(void){ return 0; }
void *  sysThreadStackTop(sys_thread_t *t){ return NULL; }
