//
let PlayState = {

    bg: null,
    gridImage: null,
    selectorImage: null,
    scoreImageL: null,
    scoreImageR: null,
    shapeStates: null,
    placeX: [],
    placeY: 950,
    shapes: [],
    selectedShape: null,
    inputPointOffset: null,
    tweenDragOffset: null,
    existsShapes: 0,
    well: null,
    isPostFindPlaces: false,
    labelScore: null,
    labelTotalScore: null,
    labelCombo: null,
    labelGameOverScore: null,
    displayScore: 0,
    buttonPause: null,
    pauseGroup: null,
    gameoverGroup: null,
    continueGroup: null,
    tutorial: null,
    curtain: null,
    ghosts: null,
    skipLabel: null,
    bestAtStart: 0,
    recordShown: false,
    gameCounted: false,
    nextHintAt: 0,

    //
    shutdown: function()
    {
        this.bg = null;
        this.gridImage = null;
        this.selectorImage = null;
        this.scoreImageL = null;
        this.scoreImageR = null;
        this.shapeStates = null;
        this.placeX.length = 0;
        this.shapes.length = 0;
        this.selectedShape = null;
        this.inputPointOffset = null;
        this.tweenDragOffset = null;
        this.existsShapes = 0;
        this.well = null;
        this.isPostFindPlaces = false;
        this.labelScore = null;
        this.labelTotalScore = null;
        this.labelCombo = null;
        this.labelGameOverScore = null;
        this.displayScore = 0;
        this.buttonPause = null;
        this.pauseGroup = null;
        this.gameoverGroup = null;
        this.continueGroup = null;
        this.tutorial = null;
        this.curtain = null;
        this.ghosts = null;
        this.skipLabel = null;
        
        //
        R.shadowCaster.clear();
    },

    //
    create: function()
    {        
        gradle.event('button_play');
        R.applyAudio();
		
		var rewardedVideoAd = true;
        let theme = R.playerData.theme === 1 ? 1 : 0;

        this.gridImage = game.add.image(21, 115, 'grid_' + theme);
        this.selectorImage = game.add.image(21, 815, 'selector_' + theme);
        this.scoreImageL = game.add.image(113, 20, 'score_' + theme);
        this.scoreImageR = game.add.image(367, 20, 'score_' + theme);
        game.add.image(263, 10, 'trone');
        game.add.image(0, 1000, 'parrot');
        this.bg = this.gridImage;

        this.well = new Well(29, 125, 10, 10, quadPadding, this, rewardedVideoAd);

        this.ghosts = [];
        for (let g = 0; g < 9; g++)
        {
            let ghost = game.add.image(0, 0, R.quad);
            ghost.anchor.set(0.5);
            ghost.visible = false;
            ghost.alpha = 0.38;
            this.well.grid.add(ghost);
            this.ghosts.push(ghost);
        }

        this.shapeStates = new ShapeStates();        

        this.placeX[0] = 115;
        this.placeX[1] = game.width / 2;
        this.placeX[2] = game.width - this.placeX[0];

        for(let i = 0; i < 3; i++)
        {            
            this.shapes.push(new Shape(9, this.placeX[i], this.placeY));
            this.shapes[i].setState(this.shapeStates.getRnd());
        }

        this.existsShapes = 3;

        //
        this.inputPointOffset = new Phaser.Point(0, 0);        

        //score
        this.displayScore = R.score = 0;
        this.bestAtStart = R.playerData.score || 0;
        this.recordShown = false;
        let strings = R.strings || {};

        R.createText(176, 96, 16, strings.score_label || 'SCORE', '#ffe7a3', true, 3);
        R.createText(432, 96, 16, strings.best_label || 'BEST', '#ffe7a3', true, 3);
        this.labelTotalScore = R.createText(432, 45, 40, this.bestAtStart.toString(), '#e0b45b');
        this.labelScore = R.createText(176, 45, 40, '0', '#e0b45b');
        this.labelCombo = R.createText(320, 96, 18, '', '#fff4c2', true, 3);

        //GUI
        //pause
        let group = game.add.group();

        let panel = game.add.image(R.BASE_GAME_WIDTH * 0.5, R.BASE_GAME_HEIGHT * 0.390, 'gui', 'panel');
        panel.anchor.set(0.5);
        group.add(panel);

        R.ui.createHomeButton(327, 430, group);
        R.ui.createThemeButton(327, 505, group, false);        
        R.ui.createSoundButton(327, 581, group);
		R.ui.createSmallPlayButton(327, 355, group, this.onPlayPause, this);

        this.pauseGroup = group;
        this.pauseGroup.visible = false;
        R.ui.buttonsEnabled(this.pauseGroup, false);

        //
        let btn = game.add.button(576, 47, 'gui', this.showPauseMenu, this, 'btn_pause', 'btn_pause', 'btn_pause_pressed', 'btn_pause');
        btn.anchor.set(0.5);
        this.buttonPause = btn;

        //game over
        group = game.add.group();
        panel = game.add.image(R.BASE_GAME_WIDTH * 0.5, 400, 'gui', 'panel');
        panel.anchor.set(0.5);
        group.add(panel);
       
        let label = R.createText(320, 250, 42, strings.no_moves_left || 'No Moves Left', '#ffffff', true, 4);
        group.add(label);

        this.labelGameOverScore = R.createText(320, 310, 32, '', '#e0b45b', true, 4);
        group.add(this.labelGameOverScore);
       
        R.ui.createBigPlayButton(322, 430, group, this.onPlayButton, this);
        R.ui.createHomeButton(322, 540, group);

        this.gameoverGroup = group;
        this.gameoverGroup.visible = false;
        R.ui.buttonsEnabled(this.gameoverGroup, false);

        //continue group
        group = game.add.group();
        panel = game.add.image(R.BASE_GAME_WIDTH * 0.5, 400, 'gui', 'REWARD');
        panel.anchor.set(0.5);
        group.add(panel);

        R.ui.createTextButton(320, 480, group, this.onWatchVideoButton, this, strings.watch_video);
        R.ui.createCancelButton(320, 565, group, this.onCancelButton, this, strings.cancel);

        this.continueGroup = group;
        this.continueGroup.visible = false;
        R.ui.buttonsEnabled(this.continueGroup, false);

        //
        this.curtain = game.add.image(-4, -4, 'gui', 'curtain');
        this.curtain.scale.set(10.1);
        this.curtain.visible = false;

        //
        game.input.onDown.add(this.inputOnDown, this);
        game.input.onUp.add(this.inputOnUp, this);

        //
        game.time.events.loop(75, this.updateLabelScore, this);
        this.nextHintAt = game.time.now + 8000;

        //
        if(R.sfx.new_game) R.sfx.new_game.play();

        //
        if(!R.playerData.tutorialCompleted)
        {
            this.tutorial = new R.Tutorial(this.well, this.shapes);
            for(let i = 0; i < 3; ++i) this.shapes[i].setState(this.shapeStates.state[this.tutorial.states[i].shapeIdx]);
            this.tutorial.start();
            this.skipLabel = R.createText(320, 758, 28, (R.strings && R.strings.skip_tutorial) || 'Skip', '#ffe08a', true, 4);
            this.skipLabel.inputEnabled = true;
            this.skipLabel.events.onInputDown.add(this.onSkipTutorial, this);
        }
        else
        {
            this.countGame();
        }
    },

    countGame: function()
    {
        if (this.gameCounted) return;
        this.gameCounted = true;
        R.playerData.gamesPlayed = (R.playerData.gamesPlayed || 0) + 1;
        R.savePlayerData();
    },

    onSkipTutorial: function()
    {
        if (!this.tutorial) return;
        if (this.well && this.well.addingCells && this.well.addingCells.length > 0) return;
        this.finishTutorial();
    },

    finishTutorial: function()
    {
        if (!this.tutorial) return;
        let tut = this.tutorial;
        this.tutorial = null;
        tut.destroy();
        if (this.well) this.well.reset();
        R.score = 0;
        this.displayScore = 0;
        if (this.labelScore) this.labelScore.text = '0';
        R.playerData.tutorialCompleted = true;
        R.savePlayerData();
        if (this.skipLabel)
        {
            this.skipLabel.destroy();
            this.skipLabel = null;
        }
        this.hideGhost();
        for (let i = 0; i < this.shapes.length; i++)
        {
            let shape = this.shapes[i];
            if (shape.isExists())
            {
                shape.setPosition(shape.startX, shape.startY);
                shape.parent.scale.set(quadScaleMin);
                shape.readyForDrag = true;
            }
        }
        this.countGame();
        if (this.existsShapes === 0) this.generateNextShapes();
    },

    hitSkip: function(x, y)
    {
        if (!this.skipLabel || !this.skipLabel.visible) return false;
        let bounds = this.skipLabel.getBounds();
        return bounds.contains(x, y);
    },

    inputOnDown: function(e)
    {        
        if(this.pauseGroup.visible || this.gameoverGroup.visible || this.continueGroup.visible) return;

        let input = game.input.activePointer;
        if (this.hitSkip(input.x, input.y)) return;

        if(input.y > R.BASE_GAME_HEIGHT || input.y < 740) return;

        //
        if(input.x < 214 && (this.tutorial == null || this.tutorial.step === 0))
        {
            if(this.shapes[0].readyForDrag) this.selectedShape = this.shapes[0];
        }
        else if(input.x < 428 && (this.tutorial == null || this.tutorial.step === 1))
        {
            if(this.shapes[1].readyForDrag) this.selectedShape = this.shapes[1];
        }
        else if(this.tutorial == null || this.tutorial.step === 2)
        {
            if(this.shapes[2].readyForDrag) this.selectedShape = this.shapes[2];
        }

        //
        if(this.selectedShape)
        {
            if(this.tutorial != null) this.tutorial.stop();

            this.inputPointOffset.x = this.selectedShape.parent.x - input.x;
            this.inputPointOffset.y = this.selectedShape.parent.y - input.y;            
            this.selectedShape.startDrag();

            if(!game.device.desktop)
            {
                this.tweenDragOffset = game.add.tween(this.inputPointOffset).to({ y: -this.selectedShape.hh }, 100, Phaser.Easing.Linear.None, true);
                this.tweenDragOffset.onComplete.add(function() { this.tweenDragOffset = null; }, this);
            }

            game.world.bringToTop(this.selectedShape.parent);
        }
    },

    inputOnUp: function(e)
    {
        if(!this.selectedShape) return;

        if(this.tweenDragOffset && this.tweenDragOffset.isRunning) this.tweenDragOffset.stop(true);        

        if(this.well.tryAddShape(this.selectedShape))
        {
            --this.existsShapes;
            if(R.sfx.put_stone) R.sfx.put_stone.play();
        }
        else
        {
            this.selectedShape.endDrag();
            if(R.sfx.error) R.sfx.error.play();
            this.blinkSelector();
            if(this.tutorial != null) this.tutorial.start();
        }
        
        this.selectedShape = null;
        this.hideGhost();
    },

    blinkSelector: function()
    {
        if (!this.selectorImage) return;
        this.selectorImage.alpha = 0.45;
        game.add.tween(this.selectorImage).to({ alpha: 1 }, 180, Phaser.Easing.Quadratic.Out, true);
    },

    hideGhost: function()
    {
        if (!this.ghosts) return;
        for (let i = 0; i < this.ghosts.length; i++) this.ghosts[i].visible = false;
    },

    updateGhost: function()
    {
        this.hideGhost();
        if (!this.selectedShape || !this.well) return;
        let cells = this.well.collectTargets(this.selectedShape);
        if (!cells) return;
        let tint = this.selectedShape.tintColor || 0xffffff;
        for (let i = 0; i < cells.length && i < this.ghosts.length; i++)
        {
            let ghost = this.ghosts[i];
            ghost.x = cells[i].x;
            ghost.y = cells[i].y;
            ghost.tint = tint;
            ghost.visible = true;
        }
    },

    generateNextShapes: function()
    {
        for(let i = 0; i < 3; ++i) this.shapes[i].reset(this.shapeStates.getRnd());

        this.existsShapes = 3;

        if(this.well.bodies.length === 0) this.findPlaces();
        else this.isPostFindPlaces = true;

        if(R.sfx.new_shapes) R.sfx.new_shapes.play();
    },

    onCompleteAddingShapeToWell: function()
    {
        if(this.existsShapes === 0) this.generateNextShapes();
        else
        {
            if(this.well.bodies.length === 0) this.findPlaces();
            else this.isPostFindPlaces = true;
        }
    },

    onLinesCleared: function(lines, combo)
    {
        if (this.tutorial) return;
        let bonus = 0;
        if (lines >= 2)
        {
            bonus = lines * 5 * combo;
            R.score += bonus;
        }
        if (combo > (R.playerData.bestCombo || 0))
        {
            R.playerData.bestCombo = combo;
            R.savePlayerData();
        }
        if (this.labelCombo) this.labelCombo.text = combo > 1 ? ((R.strings.combo || 'COMBO') + ' x' + combo) : '';
        if (lines >= 2)
        {
            R.floatText(320, 430, (R.strings.combo || 'COMBO') + ' x' + combo + '   +' + bonus, '#ffe08a', 40);
            if (R.sfx.combo) R.sfx.combo.play();
        }
    },

    onComleteRemoveLines: function()
    {
        if(this.tutorial != null)
        {
            if(++this.tutorial.step < 3) this.tutorial.start();
            else this.finishTutorial();
        }

        if(this.isPostFindPlaces)
        {
            this.isPostFindPlaces = false;
            if(this.existsShapes > 0) this.findPlaces();
        }
    },

    findPlaces: function()
    {
        if (this.tutorial) return;

        let n = 0;

        for(let i = 0; i < 3; ++i)
        {
            if(this.shapes[i].isExists())
            {
                if(this.well.findShapePlace(this.shapes[i].state)) break;
                ++n;
            }
        }

        if(n > 0 && n === this.existsShapes)
        {
            if(gradle.rewardedVideoAd) this.showContinueMenu();
            else this.showGameoverMenu();
        }
    },

    pulsePlaceableShape: function()
    {
        if (this.tutorial || this.selectedShape || !this.well) return;
        for (let i = 0; i < this.shapes.length; i++)
        {
            let shape = this.shapes[i];
            if (shape.isExists() && shape.readyForDrag && this.well.findShapePlace(shape.state))
            {
                game.add.tween(shape.parent.scale).to({ x: 0.76, y: 0.76 }, 220, Phaser.Easing.Quadratic.Out, true, 0, 0, true);
                break;
            }
        }
    },

    update: function()
    {
        if(this.selectedShape)
        {
            var input = game.input.activePointer;
            this.selectedShape.setPosition(this.inputPointOffset.x + input.x, this.inputPointOffset.y + input.y);
            this.updateGhost();
        }
        else if (!this.pauseGroup.visible && !this.gameoverGroup.visible && !this.continueGroup.visible)
        {
            if (game.time.now > this.nextHintAt)
            {
                this.pulsePlaceableShape();
                this.nextHintAt = game.time.now + 8000;
            }
        }
        else
        {
            this.nextHintAt = game.time.now + 8000;
        }
        R.shadowCaster.update();
        this.well.update();
    },

    updateLabelScore: function()
    {        
        if(this.displayScore < R.score)
        {
            ++this.displayScore;            
            this.labelScore.text = this.displayScore.toString();
            if (!this.recordShown && this.bestAtStart > 0 && this.displayScore > this.bestAtStart)
            {
                this.recordShown = true;
                R.floatText(320, 360, (R.strings && R.strings.new_record) || 'NEW RECORD', '#ffe56a', 42);
                if (R.sfx.record) R.sfx.record.play();
            }
            if (this.recordShown && this.labelTotalScore) this.labelTotalScore.text = this.displayScore.toString();
        }
    },

    setTheme: function()
    {
        let theme = R.playerData.theme === 1 ? 1 : 0;
        let suffix = '_' + theme;
        if (this.gridImage) this.gridImage.loadTexture('grid' + suffix);
        if (this.selectorImage) this.selectorImage.loadTexture('selector' + suffix);
        if (this.scoreImageL) this.scoreImageL.loadTexture('score' + suffix);
        if (this.scoreImageR) this.scoreImageR.loadTexture('score' + suffix);
        R.quad = theme === 1 ? 'quad_1' : 'quad_0';

        if (this.well)
        {
            for (let r = 0; r < this.well.rows; r++)
            {
                for (let c = 0; c < this.well.cols; c++) this.well.cells[r][c].loadTexture(R.quad);
            }
        }
        for (let i = 0; i < this.shapes.length; i++)
        {
            let quads = this.shapes[i].quads;
            for (let q = 0; q < quads.length; q++) quads[q].loadTexture(R.quad);
        }
        if (this.ghosts)
        {
            for (let g = 0; g < this.ghosts.length; g++) this.ghosts[g].loadTexture(R.quad);
        }

        if (gradle && gradle.change_background) gradle.change_background('assets/bg_play_' + theme + '.jpg');
        game.stage.backgroundColor = game.canvas.parentElement.style.backgroundColor = (theme === 1 ? '#241820' : '#102418');
        R.savePlayerData();
    },

    showPauseMenu: function()
    {
        gradle.event('button_pause');
        if(R.sfx.button) R.sfx.button.play();
        this.buttonPause.inputEnabled = false;
        this.hideGhost();

        game.world.bringToTop(this.curtain);
        game.world.bringToTop(this.pauseGroup);

        this.curtain.visible = true;
        this.pauseGroup.visible = true;

        this.curtain.alpha = 0.0;
        game.add.tween(this.curtain).to({ alpha: 1.0 }, 200, Phaser.Easing.Linear.None, true);

        this.pauseGroup.y = 600;
        game.add.tween(this.pauseGroup).to({ y: 0 }, 400, Phaser.Easing.Back.Out, true).onComplete.add(function(pauseGroup) { R.ui.buttonsEnabled(pauseGroup, true); }, this);
    },

    onPlayPause: function(button)
    {
        R.ui.buttonsEnabled(button.parent, false);

        game.add.tween(this.curtain).to({ alpha: 0.0 }, 200, Phaser.Easing.Linear.None, true);
        game.add.tween(this.pauseGroup).to({ y: 600 }, 400, Phaser.Easing.Back.In, true).onComplete.add(this.onHidePauseMenu, this);        
    },

    onHidePauseMenu: function()
    {
        this.curtain.visible = false;
        this.pauseGroup.visible = false;
        this.buttonPause.inputEnabled = true;
    },

    showGameoverMenu: function()
    {
        R.saveGame();
        this.hideGhost();

        if (this.labelGameOverScore)
        {
            let strings = R.strings || {};
            this.labelGameOverScore.text = (strings.final_score || 'Score') + '  ' + (R.score || 0) + '    ' + (strings.best_label || 'BEST') + '  ' + (R.playerData.score || 0);
        }

        game.world.bringToTop(this.curtain);
        game.world.bringToTop(this.gameoverGroup);

        this.curtain.visible = true;
        this.gameoverGroup.visible = true;

        this.curtain.alpha = 0.0;
        game.add.tween(this.curtain).to({ alpha: 1.0 }, 200, Phaser.Easing.Linear.None, true);

        this.gameoverGroup.y = 800;
        game.add.tween(this.gameoverGroup).to({ y: 0 }, 600, Phaser.Easing.Back.Out, true).onComplete.add(function(gameoverGroup) { R.ui.buttonsEnabled(gameoverGroup, true); }, this);

        if(R.sfx.new_game) R.sfx.new_game.play();
    },

    showContinueMenu: function()
    {        
        this.hideGhost();
        game.world.bringToTop(this.curtain);
        game.world.bringToTop(this.continueGroup);

        this.curtain.visible = true;
        this.continueGroup.visible = true;

        this.curtain.alpha = 0.0;
        game.add.tween(this.curtain).to({ alpha: 1.0 }, 200, Phaser.Easing.Linear.None, true);

        this.continueGroup.y = 800;
        game.add.tween(this.continueGroup).to({ y: 0 }, 600, Phaser.Easing.Back.Out, true).onComplete.add(function(continueGroup) { R.ui.buttonsEnabled(continueGroup, true); }, this);

        if(R.sfx.new_game) R.sfx.new_game.play();
    },

    onPlayButton: function(button)
    {        
        game.add.tween(this.curtain).to({ alpha: 0.0 }, 200, Phaser.Easing.Linear.None, true);
        game.add.tween(this.gameoverGroup).to({ y: 800 }, 600, Phaser.Easing.Back.In, true).onComplete.add(this.onHideGameoverMenu, this);
    },

    onHideGameoverMenu: function()
    {        
        game.state.start('play');
    },

    onWatchVideoButton: function(button)
    {
        R.ui.buttonsEnabled(this.continueGroup, false);
		gradle.playVideo(this.onCompleteAdVideo, this);
    },

    onCancelButton: function(button)
    {
		game.state.start('menu');
    },

    onCompleteAdVideo: function()
    {        
        game.paused = false;

        game.add.tween(this.curtain).to({ alpha: 0.0 }, 200, Phaser.Easing.Linear.None, true);
		if(canCredit==true){
		    R.saveGame();
			game.add.tween(this.continueGroup).to({ y: 600 }, 400, Phaser.Easing.Back.In, true).onComplete.add(this.onHideContinueMenu, this);
		}
		else{
		    game.state.start('menu');
		}
    },

    onHideContinueMenu: function()
    {
        this.curtain.visible = false;
        this.continueGroup.visible = false;

        this.well.remove3PastMoves();
        this.generateNextShapes();
    },

    onHideContinueMenuCancel: function()
    {
        this.curtain.visible = false;
        this.continueGroup.visible = false;

        this.showGameoverMenu();
    }
};
