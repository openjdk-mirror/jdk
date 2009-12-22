#ifndef PANEL_H
#define PANEL_H

#include <interface/View.h>
#include "Adaptable.h"

ADAPTABLE(Panel, (BRect frame), 
          BView, (frame, "Panel", B_FOLLOW_NONE, B_WILL_DRAW));

#endif // PANEL_H
