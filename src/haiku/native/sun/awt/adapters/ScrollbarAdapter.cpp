#include "ScrollbarAdapter.h"
#include "Scrollbar.h"
#include "java_awt_Scrollbar.h"
#include "EventEnvironment.h"
#include "KeyConversions.h"
#include <Message.h>

/* static */ Scrollbar *
ScrollbarAdapter::NewScrollbar(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	// TOTRY: pass javaObject as an argument
	jobject jjavaObject = jenv->GetObjectField(jpeer, javaObject_ID);
	DASSERT(jjavaObject != NULL);
	// TOTRY: pass min, max, direction as arguments
	float min = jenv->GetIntField(jjavaObject, minimum_ID);
	float max = jenv->GetIntField(jjavaObject, maximum_ID);
	int jorientation = jenv->GetIntField(jjavaObject, orientation_ID);
	orientation direction = (jorientation == java_awt_Scrollbar_HORIZONTAL ? B_HORIZONTAL : B_VERTICAL);
	BRect frame = GetFrame(jenv, jpeer);
	Scrollbar * value = new Scrollbar(frame, min, max, direction);
	// TOTRY: pass lineIncrement, pageIncrement as arguments
	float smallStep = jenv->GetIntField(jjavaObject, lineIncrement_ID);
	float largeStep = jenv->GetIntField(jjavaObject, pageIncrement_ID);
	value->SetSteps(smallStep, largeStep);
	// TOTRY: pass visibleAmount as an argument
	float visible = jenv->GetIntField(jjavaObject, visibleAmount_ID);
	float proportion = (min == max ? 1.0 : ((float)visible)/(max-min));
	value->SetProportion(proportion);
	// TOTRY: pass visibleAmount as an argument
	value->SetValue(jenv->GetIntField(jjavaObject, value_ID));
	return value;
}


ScrollbarAdapter::ScrollbarAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent)
	: ComponentAdapter(jenv, jpeer, jparent, NewScrollbar(jenv, jpeer, jparent)),
	  scrollbar(NULL)
{
	scrollbar = dynamic_cast<Scrollbar*>(GetView());
	DASSERT(scrollbar != NULL);
	scrollbar->SetAdapter(this);
}


ScrollbarAdapter::~ScrollbarAdapter()
{
	// TODO: determine when (if) Scrollbar should be deleted
}


// #pragma mark -
//
// JNI entry points
//

void
ScrollbarAdapter::enable()
{
	PRINT_PRETTY_FUNCTION();
	// TODO: anything?
}


void
ScrollbarAdapter::disable()
{
	PRINT_PRETTY_FUNCTION();
	// TODO: anything?
}


void
ScrollbarAdapter::setValues(int value, int visible, int minimum, int maximum)
{
	scrollbar->SetRange(minimum, maximum);
	scrollbar->SetValue(value);
	float proportion = (minimum == maximum ? 1.0 : ((float)visible)/(maximum-minimum));
	scrollbar->SetProportion(proportion);
}


void
ScrollbarAdapter::setLineIncrement(int l)
{
	float discard, largeStep;
	scrollbar->GetSteps(&discard, &largeStep);
	scrollbar->SetSteps(l, largeStep);
}


void
ScrollbarAdapter::setPageIncrement(int l)
{
	float smallStep, discard;
	scrollbar->GetSteps(&smallStep, &discard);
	scrollbar->SetSteps(smallStep, l);
}


// #pragma mark -
//
// Haiku entry points
//

/* virtual */ void
ScrollbarAdapter::ValueChanged(float value)
{
	fprintf(stderr, "%s(%f)\n", __PRETTY_FUNCTION__, value);
	EventEnvironment * environment = Environment();
	JNIEnv * jenv = environment->env;
	jobject jtarget = GetTarget(jenv);
	jobject jevent = NULL;
	// build the event
	jint jvalue = (int)value;
	if (jvalue == jenv->GetIntField(jtarget, value_ID)) {
		// nothing to do
		return;
	}
	jevent = environment->NewAdjustmentEvent(jtarget, java_awt_event_AdjustmentEvent_ADJUSTMENT_VALUE_CHANGED,
	                                                  java_awt_event_AdjustmentEvent_TRACK, jvalue, true);
	jenv->SetIntField(jtarget, value_ID, jvalue);
	jenv->DeleteLocalRef(jtarget);
	jtarget = NULL;
	// send out the appropriate event
	SendEvent(jevent);
	jenv->DeleteLocalRef(jevent);
	jevent = NULL;
}


// #pragma mark -
//
// JNI
//

#include "java_awt_Scrollbar.h"

jfieldID ScrollbarAdapter::visibleAmount_ID = NULL;
jfieldID ScrollbarAdapter::minimum_ID       = NULL;
jfieldID ScrollbarAdapter::maximum_ID       = NULL;
jfieldID ScrollbarAdapter::value_ID         = NULL;
jfieldID ScrollbarAdapter::orientation_ID   = NULL;
jfieldID ScrollbarAdapter::lineIncrement_ID = NULL;
jfieldID ScrollbarAdapter::pageIncrement_ID = NULL;

/*
 * Class:     java_awt_Scrollbar
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Scrollbar_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	ScrollbarAdapter::visibleAmount_ID = jenv->GetFieldID(jklass, "visibleAmount", "I");
	ScrollbarAdapter::minimum_ID       = jenv->GetFieldID(jklass, "minimum", "I");
	ScrollbarAdapter::maximum_ID       = jenv->GetFieldID(jklass, "maximum", "I");
	ScrollbarAdapter::value_ID         = jenv->GetFieldID(jklass, "value", "I");
	ScrollbarAdapter::orientation_ID   = jenv->GetFieldID(jklass, "orientation", "I");
	ScrollbarAdapter::lineIncrement_ID = jenv->GetFieldID(jklass, "lineIncrement", "I");
	ScrollbarAdapter::pageIncrement_ID = jenv->GetFieldID(jklass, "pageIncrement", "I");
	DASSERT(ScrollbarAdapter::visibleAmount_ID != NULL);
	DASSERT(ScrollbarAdapter::minimum_ID       != NULL);
	DASSERT(ScrollbarAdapter::maximum_ID       != NULL);
	DASSERT(ScrollbarAdapter::value_ID         != NULL);
	DASSERT(ScrollbarAdapter::orientation_ID   != NULL);
	DASSERT(ScrollbarAdapter::lineIncrement_ID != NULL);
	DASSERT(ScrollbarAdapter::pageIncrement_ID != NULL);
}

