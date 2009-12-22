//
// This file is for testing the macro-expansion of Adaptable.h.
// It is not part of the normal build.
//

#define COMPONENT_ADAPTER_H

class ComponentAdapter : public BView {};

#include "Adaptable.h"

ADAPTABLE(Label, (BRect frame, const char * text), 
          BStringView, (frame, "Label", text, B_FOLLOW_NONE, B_WILL_DRAW));
