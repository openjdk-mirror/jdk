#ifndef TEXT_VIEW_H
#define TEXT_VIEW_H

#include <interface/TextView.h>
#include "Adaptable.h"

ADAPTABLE(AbstractTextView, (BRect frame, BRect text, bool showHorizontal, bool showVertical),
          BTextView, (frame, "TextView", text, B_FOLLOW_ALL, B_FRAME_EVENTS | B_WILL_DRAW));

extern const float TEXT_INSET;

class TextView : public AbstractTextView {
private:	// constructor utility functions
	static BRect	compute_text_content_frame(BRect frame, bool showHorizontal, bool showVertical);

public: 	// initialization & destruction
	        TextView(BRect frame, bool showHorizontal, bool showVertical);
	virtual	void	FrameResized(float width, float height);
};

#endif // TEXT_VIEW_H
