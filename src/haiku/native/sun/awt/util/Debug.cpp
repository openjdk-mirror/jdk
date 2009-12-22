#include "Debug.h"
#include <support/ByteOrder.h>

void
fprint_message(FILE * file, BMessage * message)
{
	char what[5];
	*((int32*)what) = message->what;
	swap_data(B_UINT32_TYPE, (void*)what, 1, B_SWAP_BENDIAN_TO_HOST);
	what[4] = '\0';
	fprintf(file, "MESSAGE[%s]", what);
}
