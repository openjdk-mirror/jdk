#ifndef SCROLLBAR_ADAPTER_H
#define SCROLLBAR_ADAPTER_H

#include "ComponentAdapter.h"
#include <Point.h>

class Scrollbar;

class ScrollbarAdapter : public ComponentAdapter {
private:
	Scrollbar * scrollbar;

private:	// initialization
	static Scrollbar * NewScrollbar(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// initialization
	        ScrollbarAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// destruction
	virtual ~ScrollbarAdapter();

public: 	// JNI entry points
	virtual void	enable();
	virtual void	disable();
	        void    setValues(int value, int visible, int minimum, int maximum);
	        void	setLineIncrement(int l);
	        void	setPageIncrement(int l);

public: 	// Haiku entry points
	virtual void	ValueChanged(float newValue);

public: 	// -- Field and Method ID cache
	static jfieldID visibleAmount_ID;
	static jfieldID minimum_ID;
	static jfieldID maximum_ID;
	static jfieldID value_ID;
	static jfieldID orientation_ID;
	static jfieldID lineIncrement_ID;
	static jfieldID pageIncrement_ID;
};

#endif // SCROLLBAR_ADAPTER_H
