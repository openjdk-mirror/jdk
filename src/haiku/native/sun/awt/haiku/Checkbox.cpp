#include "Checkbox.h"
#include "CheckboxView.h"
#include "RadioButtonView.h"
#include <ComponentAdapter.h>
#include <interface/Window.h>

Checkbox::Checkbox(BRect frame, const char * label, bool state, bool radioMode)
	: BView(frame, "Checkbox", B_FOLLOW_NONE, B_WILL_DRAW),
	  adapter(NULL), checkbox(NULL), radioButton(NULL), radioMode(false)
{
	checkbox = new CheckboxView(Bounds(), label, state);
	radioButton = new RadioButtonView(Bounds(), label, state);
	this->radioMode = radioMode;
	if (radioMode) {
		AddChild(radioButton);
	} else {
		AddChild(checkbox);
	}
}


Checkbox::~Checkbox()
{
	// TODO: dispose of checkbox, radioButton ?
}


void
Checkbox::SetAdapter(ComponentAdapter * adapter)
{
	this->adapter = adapter;
	BWindow * window = Window();
	DASSERT(window != NULL);
	if (window->LockLooper()) {
		window->AddHandler(adapter);
		window->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
	checkbox->SetAdapter(adapter);
	radioButton->SetAdapter(adapter);
}


void
Checkbox::Draw(BRect rect) {
	BView::Draw(rect);
	adapter->DrawSurface(rect);
}


void
Checkbox::SetRadioMode(bool on)
{
	if (radioMode == on) {
		// nothing to do
		return;
	}
	if (radioMode) {
		RemoveChild(checkbox);
		AddChild(radioButton);
	} else {
		RemoveChild(radioButton);
		AddChild(checkbox);
	}
	radioMode = on;
}


const char *
Checkbox::Label()
{
	if (radioMode) {
		return radioButton->Label();
	} else {
		return checkbox->Label();
	}
}


void
Checkbox::SetLabel(const char * label)
{
	checkbox->SetLabel(label);
	radioButton->SetLabel(label);
}


int32
Checkbox::Value()
{
	if (radioMode) {
		return radioButton->Value();
	} else {
		return checkbox->Value();
	}
}


void
Checkbox::SetValue(int32 value)
{
	checkbox->SetValue(value);
	radioButton->SetValue(value);
}


void
Checkbox::SetEnabled(bool on)
{
	checkbox->SetEnabled(on);
	radioButton->SetEnabled(on);
}


