//
let R = {};
let game = null;

//
R.BASE_GAME_WIDTH = 640;
R.BASE_GAME_HEIGHT = 1060;

//
R.BANNER_HEIGHT = 62;

//
R.gameHeight = R.BASE_GAME_HEIGHT;
R.prevWindowHeight = 0;

//
R.fontName = 'YatraOne';
R.strings = null;

//
R.canAudio = false;
R.sfx = {};

//
R.score = 0;
R.sctoringEnabled = true;

R.SAVE_KEY = 'MonkeyCreative_WoodBlocks_PlayerData';

R.playerData = {
    score: 0,
    theme: 0,
    tutorialCompleted: false,
    muted: false,
    bestCombo: 0,
    gamesPlayed: 0
};

R.locale = (function ()
{
    let lang = (navigator.language || navigator.userLanguage || 'en').toLowerCase();
    if (lang.indexOf('tr') === 0) return 'tr';
    if (lang.indexOf('ru') === 0) return 'ru';
    return 'en';
})();

R.woodTints = [0xffe3b0, 0xffc56a, 0xf09a45, 0xffb483, 0xe7c07a, 0xffd48a, 0xd08a48];
R.vividTints = [0xff2424, 0xff7800, 0xffe000, 0x1ad85a, 0x1aa6ff, 0xb43dff, 0xff3a98];
R.blockTints = R.woodTints;

R.tintFor = function(state)
{
    let palette = (R.playerData && R.playerData.theme === 1) ? R.vividTints : R.woodTints;
    let span = 0;
    let filled = 0;
    if (state)
    {
        span = state.length * 3 + (state[0] ? state[0].length : 1);
        for (let j = 0; j < state.length; j++)
        {
            for (let i = 0; i < state[j].length; i++) if (state[j][i] === 1) filled++;
        }
    }
    return palette[(span + filled) % palette.length];
};

//
let startGame = function()
{
    game = new Phaser.Game(R.BASE_GAME_WIDTH, R.BASE_GAME_HEIGHT, Phaser.CANVAS, 'gameContainer', BootState, true);
};

R.nativeSave = function(key, value)
{
    try
    {
        if (window.jacob && typeof jacob.savePref === 'function') jacob.savePref(key, String(value));
    }
    catch (e) {}
};

R.nativeLoad = function(key)
{
    try
    {
        if (window.jacob && typeof jacob.loadPref === 'function')
        {
            let value = jacob.loadPref(key);
            return value == null ? '' : String(value);
        }
    }
    catch (e) {}
    return '';
};

R.floatText = function(x, y, text, color, size)
{
    let label = R.createText(x, y, size || 36, text, color || '#ffe08a', true, 4);
    let tween = game.add.tween(label).to({ y: y - 78, alpha: 0 }, 900, Phaser.Easing.Quadratic.Out, true);
    tween.onComplete.add(function() { label.destroy(); });
    return label;
};

R.ensureMusic = function()
{
    if (!R.canAudio || !game) return null;
    if (!gradle.music)
    {
        try { gradle.music = game.add.audio('music_bg', 0.42, true); }
        catch (e) { return null; }
    }
    return gradle.music;
};

// Music keeps its place. A new screen does not rewind the loop.
R.applyAudio = function()
{
    if (!game || !game.sound) return;
    let muted = !!R.playerData.muted;
    game.sound.mute = muted;
    let music = R.ensureMusic();
    if (!music) return;
    if (muted)
    {
        if (music.isPlaying) music.pause();
        return;
    }
    if (music.paused) music.resume();
    else if (!music.isPlaying) music.play();
};

//
window.onunload = function()
{
    R.saveGame();
};
