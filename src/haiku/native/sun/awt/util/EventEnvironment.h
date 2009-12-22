#ifndef EVENT_ENVIRONMENT_H
#define EVENT_ENVIRONMENT_H

#include "jni.h"
#include "java_awt_event_ActionEvent.h"
#include "java_awt_event_AdjustmentEvent.h"
#include "java_awt_event_ComponentEvent.h"
#include "java_awt_event_FocusEvent.h"
#include "java_awt_event_KeyEvent.h"
#include "java_awt_event_MouseEvent.h"
#include "java_awt_event_MouseWheelEvent.h"
#include "java_awt_event_ItemEvent.h"
#include "java_awt_event_WindowEvent.h"

class EventEnvironment {
public:
	EventEnvironment(JNIEnv * env);
	~EventEnvironment();

	jobject		NewActionEvent(jobject source, jint id, jstring command,
				               jlong when, jint modifiers);
	jobject		NewAdjustmentEvent(jobject source, jint id, jint type,
				                   jint value, jboolean isAdjusting);
	jobject		NewComponentEvent(jobject source, jint id);
	jobject		NewFocusEvent(jobject source, jint id,
				              jboolean temporary, jobject opposite);
	jobject		NewKeyEvent(jobject source, jint id, jlong when, jint modifiers,
				            jint keyCode, jchar keyChar, jint keyLocation);
	jobject		NewMouseEvent(jobject source, jint id, jlong when,
				              jint modifiers, jint x, jint y, jint clickCount,
				              jboolean popupTrigger, jint button);
	jobject		NewMouseWheelEvent(jobject source, jint id, jlong when,
				              jint modifiers, jint x, jint y, jint clickCount,
				              jboolean popupTrigger, jint scrollType,
				              jint scrollAmount, jint wheelRotation);
	jobject		NewItemEvent(jobject source, jint id,
				             jobject item, jint stateChange);
	jobject		NewSequencedEvent(jobject nested);
	jobject		NewWindowEvent(jobject source, jint id, jobject opposite,
				               jint oldState, jint newState);

	JNIEnv *	env;
private:
	jclass		ActionEventClass_ID;
	jmethodID	ActionEventConstructor_ID;
	jclass		AdjustmentEventClass_ID;
	jmethodID	AdjustmentEventConstructor_ID;
	jclass		ComponentEventClass_ID;
	jmethodID	ComponentEventConstructor_ID;
	jclass		FocusEventClass_ID;
	jmethodID	FocusEventConstructor_ID;
	jclass		KeyEventClass_ID;
	jmethodID	KeyEventConstructor_ID;
	jclass		MouseEventClass_ID;
	jmethodID	MouseEventConstructor_ID;
	jclass		MouseWheelEventClass_ID;
	jmethodID	MouseWheelEventConstructor_ID;
	jclass		ItemEventClass_ID;
	jmethodID	ItemEventConstructor_ID;
	jclass		SequencedEventClass_ID;
	jmethodID	SequencedEventConstructor_ID;
	jclass		WindowEventClass_ID;
	jmethodID	WindowEventConstructor_ID;
};

#endif // EVENT_ENVIRONMENT_H
