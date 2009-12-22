#ifndef ADAPTABLE_H
#define ADAPTABLE_H

/*
 * Example usage: 
 *
 * #include <interface/StringView.h>
 * #include "Adaptable.h"
 *
 * ADAPTABLE(Label, (BRect frame, const char * text), 
 *           BStringView, (frame, "Label", text, B_FOLLOW_NONE, B_WILL_DRAW));
 *
 */

#include "Debug.h"
#include "Utils.h"
#include <interface/Rect.h>
#include <ComponentAdapter.h>

#define ADAPTABLE(Adaptable, adaptable_args, Super, super_args) \
class Adaptable : public Super { \
protected: \
	ComponentAdapter * adapter; \
 \
public: \
	Adaptable adaptable_args : Super super_args, adapter(NULL) { \
	} \
	virtual ~Adaptable() { \
	} \
	virtual void SetAdapter(ComponentAdapter * adapter) { \
		this->adapter = adapter; \
	} \
	virtual void MessageReceived(BMessage * message) { \
		if (IsUselessMessage(message)) { \
			Super::MessageReceived(message); \
			return; \
		} \
		fprintf(stdout, "%s", __PRETTY_FUNCTION__); \
		fprint_message(stdout, message); \
		fprintf(stdout, "\n"); \
		Super::MessageReceived(message); \
		adapter->MessageReceived(message); \
	} \
	virtual void Draw(BRect rect) { \
		Super::Draw(rect); \
		adapter->DrawSurface(rect); \
	} \
	virtual void MouseDown(BPoint point) { \
		Super::MouseDown(point); \
		adapter->MouseDown(point); \
	} \
	virtual void MouseUp(BPoint point) { \
		Super::MouseUp(point); \
		adapter->MouseUp(point); \
	} \
	virtual void MouseMoved(BPoint point, uint32 code, const BMessage * message) { \
		Super::MouseMoved(point, code, message); \
		adapter->MouseMoved(point, code, message); \
	} \
	virtual void KeyDown(const char * bytes, int32 numBytes) { \
		Super::KeyDown(bytes, numBytes); \
		adapter->KeyDown(bytes, numBytes); \
	} \
	virtual void KeyUp(const char * bytes, int32 numBytes) { \
		Super::KeyUp(bytes, numBytes); \
		adapter->KeyUp(bytes, numBytes); \
	} \
}

#endif ADAPTABLE_H
