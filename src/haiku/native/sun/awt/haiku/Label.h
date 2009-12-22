#ifndef LABEL_H
#define LABEL_H

#include <interface/StringView.h>
#include "Adaptable.h"

ADAPTABLE(Label, (BRect frame, const char * text), 
          BStringView, (frame, "Label", text, B_FOLLOW_NONE, B_WILL_DRAW));

#endif // LABEL_H
