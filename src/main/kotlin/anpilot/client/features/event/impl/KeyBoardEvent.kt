package anpilot.client.features.event.impl

import anpilot.client.features.event.Cancellable

class KeyBoardEvent(val key: Int, val scancode: Int, val action: Int, val modifiers: Int) : Cancellable()
