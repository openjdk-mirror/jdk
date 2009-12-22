#ifndef LIST_VIEW_H
#define LIST_VIEW_H

#include <support/List.h>
#define LIST_H // header include problem workaround
#include <interface/ListView.h>
#include "Adaptable.h"

ADAPTABLE(AbstractListView, (BRect frame, list_view_type type),
          BListView, (frame, "ListView", type, B_FOLLOW_ALL, B_FRAME_EVENTS | B_WILL_DRAW));

class ListView : public AbstractListView {
public:
	ListView(BRect frame, list_view_type type);
	virtual void SetAdapter(ComponentAdapter * adapter);
	virtual void MessageReceived(BMessage * message);
};

#endif // LIST_VIEW_H
