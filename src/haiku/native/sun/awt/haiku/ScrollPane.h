#ifndef SCROLL_PANE_H
#define SCROLL_PANE_H

#include <interface/ScrollView.h>
#include "Adaptable.h"

extern const float CONTENT_VIEW_OUTSET;

ADAPTABLE(AbstractScrollPane, (BView * contentView, bool showHorizontal, bool showVertical),
          BScrollView, ("ScrollPane", contentView, B_FOLLOW_NONE, 0,
                        showHorizontal, showVertical, B_FANCY_BORDER));

class ScrollPane : public AbstractScrollPane {
private:
	BView * contentView;

public:
	BView * GetContentView() { return contentView; }

private:	// constructor utility functions
	static BRect	compute_content_view_frame(BRect scrollFrame, bool showHorizontal, bool showVertical);
	static BView *	NewContentView(BRect scrollFrame, bool showHorizontal, bool showVertical);

public: 	// initialization & destruction
	        ScrollPane(BRect frame, bool showScrollBars);
	virtual ~ScrollPane();
	virtual void SetAdapter(ComponentAdapter * adapter);
};

#endif // SCROLL_PANE_H
