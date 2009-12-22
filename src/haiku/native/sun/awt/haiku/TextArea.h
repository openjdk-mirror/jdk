#ifndef TEXT_AREA_H
#define TEXT_AREA_H

#include "ScrollPane.h"

class TextView;

class TextArea : public AbstractScrollPane {
private:
	TextView * textView;

public: 	// accessor
	TextView * GetTextView() { return textView; }

private:	// constructor utility functions
	static BRect	compute_text_view_frame(BRect scrollFrame, bool showHorizontal, bool showVertical);
	static TextView * NewTextView(BRect scrollFrame, bool showHorizontal, bool showVertical);

public: 	// initialization & destruction
	        TextArea(BRect frame, bool showHorizontal, bool showVertical);
	virtual ~TextArea();
	virtual void SetAdapter(ComponentAdapter * adapter);
};

#endif // TEXT_AREA_H
