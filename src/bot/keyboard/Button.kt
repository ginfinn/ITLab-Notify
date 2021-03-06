package bot.keyboard

import bot.keyboard.properties.Color

class Button {

    private val action: Action
    private val color: String

    constructor(action: Action, color: Color) {
        this.action = action
        this.color = color.type
    }

}