//
R.createText = function(x, y, size, text, fill, isStroke, strokeThicknes, isWrap)
{
    if(!fill) fill = '#ffffff';
    let label = game.add.text(x, y, text);
    label.font = R.fontName + ', Arial, sans-serif';
    label.anchor.set(0.5);
    label.align = 'center';
    label.fontSize = size;
    label.fill = fill;

    if(isStroke)
    {
        label.stroke = '#000000';
        label.strokeThickness = strokeThicknes;
    }
    if(isWrap)
    {
        label.wordWrap = true;
        label.wordWrapWidth = game.width - 10;
    }
    return label;
};

//GUI
R.ui = {

    btnTheme: null,
    btnSound: null,

    buttonsEnabled: function(group, enabled)
    {
        let i = group.total;
        while(i--) if(group.children[i].type === Phaser.BUTTON) group.children[i].inputEnabled = enabled;
        
    },

    //
    createHomeButton: function(x, y, group)
    {
        let btn = game.add.button(x, y, 'gui', this.onHomeButton, this, 'btn_home', 'btn_home', 'btn_home_pressed', 'btn_home');
        btn.anchor.set(0.5);
        if(R.sfx.button) btn.setDownSound(R.sfx.button);
        if(group) group.add(btn);        
    },

    onHomeButton: function()
    {
        R.saveGame();

        gradle.event('button_home');

        game.state.start('menu');        
    },

    createThemeButton: function(x, y, group, big)
    {
        let frameNormal = R.playerData.theme === 0 ? 'btn_lamp_on' : 'btn_lamp_off';
        let framePressed = R.playerData.theme === 0 ? 'btn_lamp_on_pressed' : 'btn_lamp_off_pressed';

        if(big)
        {
            frameNormal += '_big';
            framePressed += '_big';
        }

        let btn = game.add.button(x, y, 'gui', this.onThemeButton, this, frameNormal, frameNormal, framePressed, frameNormal);
        btn.anchor.set(0.5);
        if(R.sfx.button) btn.setDownSound(R.sfx.button);
        if(group) group.add(btn);
        this.btnTheme = btn;
        this.btnTheme.big = big;
    },

    onThemeButton: function()
    {
		R.playerData.theme = 1 - R.playerData.theme;
        R.quad = R.playerData.theme === 1 ? 'quad_1' : 'quad_0';
        R.savePlayerData();

        let frameNormal = R.playerData.theme === 0 ? 'btn_lamp_on' : 'btn_lamp_off';
        let framePressed = R.playerData.theme === 0 ? 'btn_lamp_on_pressed' : 'btn_lamp_off_pressed';

        if(this.btnTheme.big)
        {
            frameNormal += '_big';
            framePressed += '_big';
        }

        this.btnTheme.setFrames(frameNormal, frameNormal, framePressed, frameNormal);

        let state = game.state.getCurrentState();
        if(state.setTheme) state.setTheme();
    },

    createSoundButton: function(x, y, group)
    {
        let frameNormal = game.sound.mute ? 'btn_sound_off' : 'btn_sound_on';
        let framePressed = game.sound.mute ? 'btn_sound_off_pressed' : 'btn_sound_on_pressed';

        let btn = game.add.button(x, y, 'gui', this.onSoundButton, this, frameNormal, frameNormal, framePressed, frameNormal);
        btn.anchor.set(0.5);
        if(R.sfx.button) btn.setDownSound(R.sfx.button);
        if(group) group.add(btn);
        this.btnSound = btn;

        btn.visible = R.canAudio;
    },

    onSoundButton: function()
    {
        game.sound.mute = !game.sound.mute;
        R.playerData.muted = game.sound.mute;
        R.savePlayerData();
        R.applyAudio();

        let frameNormal = game.sound.mute ? 'btn_sound_off' : 'btn_sound_on';
        let framePressed = game.sound.mute ? 'btn_sound_off_pressed' : 'btn_sound_on_pressed';

        this.btnSound.setFrames(frameNormal, frameNormal, framePressed, frameNormal);
    },

    createSmallPlayButton: function(x, y, group, callback, context)
    {
        let btn = game.add.button(x, y, 'gui', callback, context, 'btn_small_play', 'btn_small_play', 'btn_small_play_pressed', 'btn_small_play');
        btn.anchor.set(0.5);
        if(R.sfx.button) btn.setDownSound(R.sfx.button);
        if(group) group.add(btn);
    },

    createBigPlayButton: function(x, y, group, callback, context)
    {
        let btn = game.add.button(x, y, 'gui', callback, context, 'btn_big_play', 'btn_big_play', 'btn_big_play_pressed', 'btn_big_play');
        btn.anchor.set(0.5);
        btn.inputEnabled = true;
        if(R.sfx.button) btn.setDownSound(R.sfx.button);
        if(group) group.add(btn);
        return btn;
    },

    createTextButton: function(x, y, group, callback, context, string)
    {
        let btn = game.add.button(x, y, 'gui', callback, context, 'button', 'button', 'button_pressed', 'button');
        btn.anchor.set(0.5);
        if(R.sfx.button) btn.setDownSound(R.sfx.button);
        if(group) group.add(btn);

        if(string)
        {
            let label = R.createText(0, -10, 45, string, '#ffffff', true, 4);
            label.stroke = '#000000';
            btn.addChild(label);
        }
    },
	
	createCancelButton: function(x, y, group, callback, context)
    {
        let btn = game.add.button(x, y, 'gui', callback, context, 'button_cancel', 'button_cancel', 'button_cancel_pressed', 'button_cancel');
        btn.anchor.set(0.5);
        if(R.sfx.button) btn.setDownSound(R.sfx.button);
        if(group) group.add(btn);
    }
};