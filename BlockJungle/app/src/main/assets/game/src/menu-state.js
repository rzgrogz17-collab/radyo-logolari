//
let MenuState = {

    shutdown: function()
    {
    },

    //
    create: function()
    {
        let strings = R.strings || {};

        // The home art already carries the logo, so the score sits in the open gap under it.
        let stat = (strings.best_label || 'BEST') + '  ' + (R.playerData.score || 0);
        if ((R.playerData.bestCombo || 0) > 1) stat += '    ' + (strings.combo || 'COMBO') + '  ' + R.playerData.bestCombo;
        if ((R.playerData.gamesPlayed || 0) > 0) stat += '    ' + (strings.games || 'GAMES') + '  ' + R.playerData.gamesPlayed;
        R.createText(320, 548, 26, stat, '#fff6d2', true, 4);

        if (!R.playerData.tutorialCompleted)
        {
            R.createText(320, 760, 26, strings.drag_hint || '', '#ffffff', true, 3);
        }

        R.ui.createSoundButton(230, 620, game.world);
        R.ui.createThemeButton(410, 620, game.world, false);
        R.ui.createBigPlayButton(320, 900, game.world, this.onPlayButton, this);

        let playButton = game.world.getChildAt(game.world.children.length - 1);
        if (playButton && playButton.scale)
        {
            game.add.tween(playButton.scale).to({ x: 1.06, y: 1.06 }, 700, Phaser.Easing.Sinusoidal.InOut, true, 0, -1, true);
        }

        this.setTheme();
        R.applyAudio();

        gradle.event('page_menu');
    },

    onPlayButton: function()
    {
        game.state.start('play');
    },

    setTheme: function()
    {
        let theme = R.playerData.theme === 1 ? 1 : 0;
        if (gradle && gradle.change_background) gradle.change_background('assets/bg_home_' + theme + '.jpg');
        let fallback = theme === 1 ? '#241820' : '#102418';
        game.stage.backgroundColor = game.canvas.parentElement.style.backgroundColor = fallback;
    }
};
