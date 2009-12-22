#ifndef LIST_ADAPTER_H
#define LIST_ADAPTER_H

#include "ComponentAdapter.h"
#include <Point.h>
#include <String.h>
#include <vector>

class List;

class ListAdapter : public ComponentAdapter {
private:
	List * list;

	// selection state
	BString oldString;

private:	// initialization
	static List * NewList(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// initialization
	        ListAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// destruction
	virtual ~ListAdapter();

public: 	// JNI entry points
	virtual void	enable();
	virtual void	disable();
	virtual BPoint	getPreferredSize();
	virtual BPoint	getMinimumSize();
	        std::vector<int>	getSelectedIndexes();
	        void	add(char * item, int index);
	        void	delItems(int start, int end);
	        void	removeAll();
	        void	select(int index);
	        void	deselect(int index);
	        void	makeVisible(int index);
	        void	setMultipleMode(bool b);
	        BPoint	getPreferredSize(int rows);
	        BPoint	getMinimumSize(int rows);

private: 	// Utility function
	std::vector<const char *> parseStrings(BMessage * message);

protected:	// BInvoker "entry points"
	virtual void	InvocationReceived(BMessage * message);
	virtual void	SelectionReceived(BMessage * message);

public: 	// -- Field and Method ID cache
	static jfieldID rows_ID;
	static jfieldID multipleMode_ID;
	static jfieldID visibleIndex_ID;
};

#endif // LIST_ADAPTER_H
