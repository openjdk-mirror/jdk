#ifndef CANVAS_H
#define CANVAS_H

#include <interface/View.h>
#include "Adaptable.h"

ADAPTABLE(Canvas, (BRect frame), 
          BView, (frame, "Canvas", B_FOLLOW_NONE, B_WILL_DRAW));

#endif // CANVAS_H
