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

R.blockTints = [0xff4d4d, 0xff8a1e, 0xffd23a, 0x3dce5a, 0x3eb6ff, 0x9b6bff, 0xff5eab];

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
