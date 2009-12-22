#ifndef CHOICE_ADAPTER_H
#define CHOICE_ADAPTER_H

#include "ComponentAdapter.h"
#include <interface/Point.h>

class Choice;

class ChoiceAdapter : public ComponentAdapter {
private:
	Choice * choice;

private:	// initialization
	static Choice * NewChoice(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// initialization
	        ChoiceAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// destruction
	virtual ~ChoiceAdapter();

public: 	// JNI entry points
	virtual void	enable();
	virtual void	disable();
	        void	add(char * item, int index);
	        void	remove(int index);
	        void	removeAll();
	        void	select(int index);
	        BPoint	getMinimumSize();

protected:	// BInvoker "entry points"
	virtual void	InvocationReceived(BMessage * message);

public: 	// -- Field and Method ID cache
	static jfieldID selectedIndex_ID;
};

#endif // CHOICE_ADAPTER_H
