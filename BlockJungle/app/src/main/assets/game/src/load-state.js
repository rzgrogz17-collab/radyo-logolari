//
let LoadState = {
    
    loadingBarFull: null,
    loadText: null,
    tipText: null,
    sfx_key: null,
    tips: null,
    tipIndex: 0,
    shownAt: 0,
    fileProgress: 0,
    filesReady: false,
    entered: false,
    enterScheduled: false,
    minLoadMs: 3000,

    //
    init: function()
    {
        game.load.onFileComplete.add(this.fileComplete, this);
        game.load.onLoadComplete.add(this.loadComplete, this);
    },

    //
    create: function()
    {      
        game.load.image('transparent', 'assets/transparent.png');

        game.load.image('trone', 'assets/trone.png');
        game.load.image('parrot', 'assets/parrot.png');

        game.load.image('bg_play_0', 'assets/bg_play_0.jpg');
        game.load.image('bg_play_1', 'assets/bg_play_1.jpg');
		
        game.load.image('selector_0', 'assets/selector_0.jpg');
        game.load.image('selector_1', 'assets/selector_1.png');
		
        game.load.image('score_0', 'assets/score_0.png');
        game.load.image('score_1', 'assets/score_1.png');
		
        game.load.image('grid_0', 'assets/grid_0.png');
        game.load.image('grid_1', 'assets/grid_1.png');

        game.load.image('quad_0', 'assets/quad_0.png');
        game.load.image('quad_1', 'assets/quad_1.png');
        game.load.image('quad_wood', 'assets/quad_wood.png');
        game.load.image('quad_bone', 'assets/quad_bone.png');

        game.load.image('quad_shadow', 'assets/quad_shadow.png');

        game.load.atlas('gui', 'assets/gui.png', 'assets/gui.json');

        let lang = R.locale || 'en';
        game.load.json('strings', 'text/' + lang + '.json');
        game.load.json('strings_en', 'text/en.json');

        //sfx
        if(R.canAudio)
        {
            let sfx = ['error', 'new_shapes', 'new_game', 'put_stone', 'row_removed', 'button', 'combo', 'record'];
            for(var i in sfx) game.load.audio(sfx[i], ['assets/sfx/' + sfx[i] + '.ogg', 'assets/sfx/' + sfx[i] + '.mp3']);
            this.sfx_key = sfx;
            game.load.audio('music_bg', ['assets/sfx/bg.ogg', 'assets/sfx/bg.mp3']);
        }
        
        //
        this.shownAt = Date.now();
        this.fileProgress = 0;
        this.filesReady = false;
        this.entered = false;
        this.enterScheduled = false;
        this.createEnvironment();
        game.time.events.loop(40, this.paintProgress, this);

        if (gradle && gradle.event_ext) gradle.event_ext('hide_splash');

        //
        game.load.start();
    },  

    shutdown: function()
    {        
        this.loadingBarFull = null;
        this.loadText = null;
        this.tipText = null;
        this.sfx_key = null;
        this.tips = null;
    },

    createEnvironment: function()
    {
        game.stage.backgroundColor = game.canvas.parentElement.style.backgroundColor = '#0c1a16';

        let cx = game.world.centerX;

        let logo = game.add.image(320, 430, 'loading', 'logo');
        logo.anchor.set(0.5);
        game.add.tween(logo).to({ y: 418 }, 900, Phaser.Easing.Sinusoidal.InOut, true, 0, -1, true);
        game.add.tween(logo.scale).to({ x: 1.04, y: 1.04 }, 900, Phaser.Easing.Sinusoidal.InOut, true, 0, -1, true);

        this.loadingBarFull = game.add.image(cx - 149, 560, 'loading', 'bar_full');
        let cropRect = new Phaser.Rectangle(0, 0, 0, 53);
        this.loadingBarFull.crop(cropRect);

        let loadingBar = game.add.image(cx - 156, 554, 'loading', 'bar_empty');

        let loadingWord = R.locale === 'tr' ? 'Yükleniyor' : (R.locale === 'ru' ? 'Загрузка' : 'Loading');
        this.loadText = R.createText(cx, loadingBar.y + 78, 36, loadingWord + '  0%', '#ffffff', true, 4);

        this.tips = R.locale === 'tr'
            ? ['Parçayı tahtadaki boşluğa sürükle', 'Dolu satır ve sütunlar temizlenir', 'Kombo, birden fazla sırayı birden silince büyür', 'Eğitim yalnızca ilk oyunda çıkar']
            : (R.locale === 'ru'
                ? ['Перетащи фигуру в свободную клетку', 'Полные ряды и столбцы исчезают', 'Комбо растёт, если снять несколько линий сразу', 'Обучение показывается только один раз']
                : ['Drag a piece into an open gap', 'Full rows and columns clear away', 'Combos grow when several lines clear at once', 'The how-to-play is shown only once']);
        this.tipIndex = 0;
        this.tipText = R.createText(cx, loadingBar.y + 140, 26, this.tips[0], '#d8ffe4', true, 3);
        game.time.events.loop(1600, this.nextTip, this);
    },

    nextTip: function()
    {
        if (!this.tipText || !this.tips) return;
        this.tipIndex = (this.tipIndex + 1) % this.tips.length;
        this.tipText.setText(this.tips[this.tipIndex]);
        this.tipText.alpha = 0;
        game.add.tween(this.tipText).to({ alpha: 1 }, 280, Phaser.Easing.Quadratic.Out, true);
    },

    //
    fileComplete: function(progress, cacheKey, success, totalLoaded, totalFiles)
    {
        this.fileProgress = progress;
        this.paintProgress();
    },

    paintProgress: function()
    {
        if (!this.loadingBarFull || this.entered) return;
        let timeProgress = Math.min(100, ((Date.now() - this.shownAt) / this.minLoadMs) * 100);
        let shown = this.filesReady ? timeProgress : Math.min(this.fileProgress, timeProgress);
        shown = Math.max(0, Math.min(100, shown));
        this.loadingBarFull.cropRect.width = 315 * shown * 0.01;
        this.loadingBarFull.updateCrop();
        let word = R.locale === 'tr' ? 'Yükleniyor' : (R.locale === 'ru' ? 'Загрузка' : 'Loading');
        if (this.loadText) this.loadText.setText(word + '  ' + Math.floor(shown) + '%');
    },

    //
    loadComplete: function()
    {        
        //sfx
        if(R.canAudio)
        {
            for(let i in this.sfx_key) R.sfx[this.sfx_key[i]] = game.add.audio(this.sfx_key[i]);
            gradle.music = game.add.audio('music_bg', 0.42, true);
        }
        
        // 
        try { R.strings = game.cache.getJSON('strings'); }
        catch (e) { R.strings = null; }
        if (!R.strings)
        {
            try { R.strings = game.cache.getJSON('strings_en'); }
            catch (e2) { R.strings = null; }
        }
        if (!R.strings) R.strings = {};

        //
        R.loadGame();
        this.filesReady = true;
        this.paintProgress();
        this.tryEnterMenu();
    },

    tryEnterMenu: function()
    {
        if (this.entered || !this.filesReady) return;
        let wait = this.minLoadMs - (Date.now() - this.shownAt);
        if (wait > 16)
        {
            if (!this.enterScheduled)
            {
                this.enterScheduled = true;
                game.time.events.add(wait, function()
                {
                    this.enterScheduled = false;
                    this.tryEnterMenu();
                }, this);
            }
            return;
        }
        this.entered = true;
        this.paintProgress();
        game.state.start('menu');
    }
};

//
R.savePlayerData = function()
{
    let json = JSON.stringify(R.playerData);
    try
    {
        if (game && game.device.localStorage) localStorage.setItem(R.SAVE_KEY, json);
    }
    catch (e) {}
    R.nativeSave('player_data', json);
    if (R.playerData.tutorialCompleted)
    {
        try
        {
            if (game && game.device.localStorage) localStorage.setItem('bj_tutorial_done', '1');
        }
        catch (e) {}
        R.nativeSave('tutorial_done', '1');
    }
};

//
R.saveGame = function()
{
    if ((R.score || 0) > (R.playerData.score || 0)) R.playerData.score = R.score;
    R.savePlayerData();
};

//
R.loadGame = function()
{
    let raw = R.nativeLoad('player_data');
    if (!raw && game.device.localStorage) raw = localStorage.getItem(R.SAVE_KEY) || '';
    if (raw)
    {
        try
        {
            let data = JSON.parse(raw);
            if (data && typeof data === 'object')
            {
                for (let k in data)
                {
                    if (Object.prototype.hasOwnProperty.call(data, k)) R.playerData[k] = data[k];
                }
            }
        }
        catch (e) {}
    }

    if (R.nativeLoad('tutorial_done') === '1') R.playerData.tutorialCompleted = true;
    else if (game.device.localStorage && localStorage.getItem('bj_tutorial_done') === '1') R.playerData.tutorialCompleted = true;

    R.playerData.tutorialCompleted = !!R.playerData.tutorialCompleted;
    R.playerData.muted = !!R.playerData.muted;
    if (typeof R.playerData.score !== 'number' || isNaN(R.playerData.score)) R.playerData.score = 0;
    if (typeof R.playerData.bestCombo !== 'number' || isNaN(R.playerData.bestCombo)) R.playerData.bestCombo = 0;
    if (typeof R.playerData.gamesPlayed !== 'number' || isNaN(R.playerData.gamesPlayed)) R.playerData.gamesPlayed = 0;
    if (R.playerData.theme !== 0 && R.playerData.theme !== 1) R.playerData.theme = 0;

    R.quad = R.cubeKey ? R.cubeKey() : (R.playerData.theme === 1 ? 'quad_wood' : 'quad_0');
    if (game && game.sound) game.sound.mute = R.playerData.muted;
};
