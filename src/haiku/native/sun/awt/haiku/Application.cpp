#include "Application.h"

#include <AppDefs.h>
#include <Message.h>
#include <cstdio>

Application::Application() :
	BApplication(JAVA_APP_SIG){}

Application::~Application() {}

void Application::Quit() {
	if (QuitRequested()) {
		BApplication::Quit();
	}
}
