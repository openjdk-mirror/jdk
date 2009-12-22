#include "EventEnvironment.h"
#include "Debug.h"
#include <cassert>

EventEnvironment::EventEnvironment(JNIEnv * env)
{
	assert(env != NULL);
	this->env = env;

	ActionEventClass_ID
		= (jclass)env->NewGlobalRef(env->FindClass("java/awt/event/ActionEvent"));
	ActionEventConstructor_ID
		= env->GetMethodID(ActionEventClass_ID, "<init>",
		                   "(Ljava/lang/Object;ILjava/lang/String;JI)V");

	AdjustmentEventClass_ID
		= (jclass)env->NewGlobalRef(env->FindClass("java/awt/event/AdjustmentEvent"));
	AdjustmentEventConstructor_ID
		= env->GetMethodID(AdjustmentEventClass_ID, "<init>",
		                   "(Ljava/awt/Adjustable;IIIZ)V");

	ComponentEventClass_ID
		= (jclass)env->NewGlobalRef(env->FindClass("java/awt/event/ComponentEvent"));
	ComponentEventConstructor_ID
		= env->GetMethodID(ComponentEventClass_ID, "<init>",
		                   "(Ljava/awt/Component;I)V");
	
	FocusEventClass_ID
		= (jclass)env->NewGlobalRef(env->FindClass("java/awt/event/FocusEvent"));
	FocusEventConstructor_ID
		= env->GetMethodID(FocusEventClass_ID, "<init>",
		                   "(Ljava/awt/Component;IZLjava/awt/Component;)V");

	KeyEventClass_ID
		= (jclass)env->NewGlobalRef(env->FindClass("java/awt/event/KeyEvent"));
	KeyEventConstructor_ID
		= env->GetMethodID(KeyEventClass_ID, "<init>",
		                   "(Ljava/awt/Component;IJIIC)V");

	MouseEventClass_ID
		= (jclass)env->NewGlobalRef(env->FindClass("java/awt/event/MouseEvent"));
	MouseEventConstructor_ID
		= env->GetMethodID(MouseEventClass_ID, "<init>",
		                   "(Ljava/awt/Component;IJIIIIZI)V");

	MouseWheelEventClass_ID
		= (jclass)env->NewGlobalRef(env->FindClass("java/awt/event/MouseWheelEvent"));
	MouseWheelEventConstructor_ID
		= env->GetMethodID(MouseWheelEventClass_ID, "<init>",
		                   "(Ljava/awt/Component;IJIIIIZIII)V");

	ItemEventClass_ID
		= (jclass)env->NewGlobalRef(env->FindClass("java/awt/event/ItemEvent"));
	ItemEventConstructor_ID
		= env->GetMethodID(ItemEventClass_ID, "<init>",
		                   "(Ljava/awt/ItemSelectable;ILjava/lang/Object;I)V");

	SequencedEventClass_ID
		= (jclass)env->NewGlobalRef(env->FindClass("java/awt/SequencedEvent"));
	SequencedEventConstructor_ID
		= env->GetMethodID(SequencedEventClass_ID, "<init>",
		                   "(Ljava/awt/AWTEvent;)V");

	WindowEventClass_ID
		= (jclass)env->NewGlobalRef(env->FindClass("java/awt/event/WindowEvent"));
	WindowEventConstructor_ID
		= env->GetMethodID(WindowEventClass_ID, "<init>", 
		                   "(Ljava/awt/Window;ILjava/awt/Window;II)V");

}


EventEnvironment::~EventEnvironment()
{
	if (ActionEventClass_ID != NULL) {
		env->DeleteGlobalRef(ActionEventClass_ID);
	}
	if (AdjustmentEventClass_ID != NULL) {
		env->DeleteGlobalRef(AdjustmentEventClass_ID);
	}
	if (ComponentEventClass_ID != NULL) {
		env->DeleteGlobalRef(ComponentEventClass_ID);
	}
	if (FocusEventClass_ID != NULL) {
		env->DeleteGlobalRef(FocusEventClass_ID);
	}
	if (ItemEventClass_ID != NULL) {
		env->DeleteGlobalRef(ItemEventClass_ID);
	}
	if (KeyEventClass_ID != NULL) {
		env->DeleteGlobalRef(KeyEventClass_ID);
	}
	if (MouseEventClass_ID != NULL) {
		env->DeleteGlobalRef(MouseEventClass_ID);
	}
	if (MouseWheelEventClass_ID != NULL) {
		env->DeleteGlobalRef(MouseWheelEventClass_ID);
	}
	if (SequencedEventClass_ID != NULL) {
		env->DeleteGlobalRef(SequencedEventClass_ID);
	}
	if (WindowEventClass_ID != NULL) {
		env->DeleteGlobalRef(WindowEventClass_ID);
	}
}


jobject
EventEnvironment::NewActionEvent(jobject source, jint id, jstring command,
                               jlong when, jint modifiers)
{
	DASSERT(source != NULL);
	return env->NewObject(ActionEventClass_ID, ActionEventConstructor_ID,
	                      source, id, command, when, modifiers);
}


jobject
EventEnvironment::NewAdjustmentEvent(jobject source, jint id, jint type,
                                   jint value, jboolean isAdjusting)
{
	DASSERT(source != NULL);
	return env->NewObject(AdjustmentEventClass_ID, AdjustmentEventConstructor_ID,
	                      source, id, type, value, isAdjusting);
}


jobject
EventEnvironment::NewComponentEvent(jobject source, jint id)
{
	DASSERT(source != NULL);
	return env->NewObject(ComponentEventClass_ID, ComponentEventConstructor_ID,
	                      source, id);
}


jobject
EventEnvironment::NewFocusEvent(jobject source, jint id,
                                jboolean temporary, jobject opposite)
{
	DASSERT(source != NULL);
	return env->NewObject(FocusEventClass_ID, FocusEventConstructor_ID,
	                      source, id, temporary, opposite);
}


jobject
EventEnvironment::NewItemEvent(jobject source, jint id,
                             jobject item, jint stateChange)
{
	DASSERT(source != NULL);
	return env->NewObject(ItemEventClass_ID, ItemEventConstructor_ID,
	                      source, id, item, stateChange);
}


jobject
EventEnvironment::NewKeyEvent(jobject source, jint id, jlong when, jint modifiers,
                            jint keyCode, jchar keyChar, jint keyLocation)
{
	DASSERT(source != NULL);
	return env->NewObject(KeyEventClass_ID, KeyEventConstructor_ID,
	                      source, id, when, modifiers, keyCode, keyChar, keyLocation);
}


jobject
EventEnvironment::NewMouseEvent(jobject source, jint id, jlong when,
                              jint modifiers, jint x, jint y, jint clickCount,
                              jboolean popupTrigger, jint button)
{
	DASSERT(source != NULL);
	return env->NewObject(MouseEventClass_ID, MouseEventConstructor_ID,
	                      source, id, when, modifiers, x, y,
	                      clickCount, popupTrigger, button);
}


jobject
EventEnvironment::NewMouseWheelEvent(jobject source, jint id, jlong when,
                                   jint modifiers, jint x, jint y, jint clickCount,
                                   jboolean popupTrigger, jint scrollType,
                                   jint scrollAmount, jint wheelRotation)
{
	DASSERT(source != NULL);
	return env->NewObject(MouseWheelEventClass_ID, MouseWheelEventConstructor_ID,
	                      source, id, when, modifiers, x, y,
	                      clickCount, popupTrigger, scrollType,
	                      scrollAmount, wheelRotation);
}


jobject
EventEnvironment::NewSequencedEvent(jobject nested)
{
	return env->NewObject(SequencedEventClass_ID, SequencedEventConstructor_ID,
	                      nested);
}


jobject
EventEnvironment::NewWindowEvent(jobject source, jint id, jobject opposite,
                               jint oldState, jint newState)
{
	DASSERT(source != NULL);
	return env->NewObject(WindowEventClass_ID, WindowEventConstructor_ID,
	                      source, id, opposite, oldState, newState);
}
