#ifndef LIST_H
#define LIST_H

#include <support/List.h>
#include <interface/ListView.h>
#include "ScrollPane.h"

class ListView;

class List : public AbstractScrollPane {
private:
	ListView * listView;

public: 	// accessor
	ListView * GetListView() { return listView; }

private:	// constructor utility functions
	static BRect	compute_list_view_frame(BRect scrollFrame, bool showHorizontal, bool showVertical);
	static ListView * NewListView(BRect scrollFrame, list_view_type type);

public: 	// initialization & destruction
	        List(BRect frame, list_view_type type);
	virtual ~List();
	virtual void SetAdapter(ComponentAdapter * adapter);
};

#endif // LIST_H
