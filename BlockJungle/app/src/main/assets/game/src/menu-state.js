//
let MenuState = {

    shutdown: function()
    {
    },

    //
    create: function()
    {
        let strings = R.strings || {};

        // Center of the open lake, above the bottom flowers and the ad banner.
        let playButton = R.ui.createBigPlayButton(320, 590, game.world, this.onPlayButton, this);
        if (playButton)
        {
            game.world.bringToTop(playButton);
            game.add.tween(playButton.scale).to({ x: 1.08, y: 1.08 }, 700, Phaser.Easing.Sinusoidal.InOut, true, 0, -1, true);
        }

        let stat = (strings.best_label || 'BEST') + '  ' + (R.playerData.score || 0);
        if ((R.playerData.bestCombo || 0) > 1) stat += '    ' + (strings.combo || 'COMBO') + '  ' + R.playerData.bestCombo;
        if ((R.playerData.gamesPlayed || 0) > 0) stat += '    ' + (strings.games || 'GAMES') + '  ' + R.playerData.gamesPlayed;
        R.createText(320, 430, 26, stat, '#fff6d2', true, 4);

        R.ui.createSoundButton(150, 820, game.world);
        R.ui.createThemeButton(490, 820, game.world, false);

        if (!R.playerData.tutorialCompleted)
        {
            R.createText(320, 760, 24, strings.drag_hint || '', '#ffffff', true, 3);
        }

        if (playButton) game.world.bringToTop(playButton);

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
