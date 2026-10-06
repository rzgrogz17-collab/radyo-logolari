//
R.TutState = function()
{
    this.well = [];
    this.shapeIdx = 0;
    this.point1 = new Phaser.Point(0, 0);
    this.point2 = new Phaser.Point(0, 0);
}

R.TutState.prototype.constructor = R.TutState;

//
R.Tutorial = function(well, shapes)
{
    this.well = well;
    this.shapes = shapes;
    this.step = 0;

    this.circle = game.add.image(0, 0, 'gui', 'tut_circle');
    this.circle.anchor.set(0.5);

    this.finger = game.add.image(0, 0, 'gui', 'finger');
    
    this._tweens = [];
    this.states = [];

    let state = new R.TutState();

    //1
    state.well = [1, 1, 1, 2, 2, 2, 1, 1, 1, 1,
                  1, 1, 1, 2, 2, 2, 1, 1, 1, 1,
                  1, 1, 1, 2, 2, 2, 1, 1, 1, 1,
                  1, 1, 1, 2, 2, 2, 1, 1, 1, 1,
                  1, 1, 1, 2, 2, 2, 1, 1, 1, 1,
                  1, 1, 1, 0, 0, 0, 1, 1, 1, 1,
                  1, 1, 1, 2, 2, 2, 1, 1, 1, 1,
                  1, 1, 1, 2, 2, 2, 1, 1, 1, 1,
                  1, 1, 1, 2, 2, 2, 1, 1, 1, 1,
                  1, 1, 1, 2, 2, 2, 1, 1, 1, 1];

    state.shapeIdx = 3;

    state.point1.set(115, 850);
    state.point2.set(290, 444);

    this.states.push(state);

    //2
    state = new R.TutState();
    state.well = [1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                  1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                  1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                  2, 2, 2, 2, 0, 2, 2, 2, 2, 2,
                  2, 2, 2, 2, 0, 2, 2, 2, 2, 2,
                  2, 2, 2, 2, 0, 2, 2, 2, 2, 2,
                  1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                  1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                  1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                  1, 1, 1, 1, 1, 1, 1, 1, 1, 1];

    state.shapeIdx = 4;

    state.point1.set(320, 850);
    state.point2.set(290, 386);

    this.states.push(state);

    //3
    state = new R.TutState();
    state.well = [1, 1, 1, 2, 2, 2, 1, 1, 1, 1,
                  1, 1, 1, 2, 2, 2, 1, 1, 1, 1,
                  1, 1, 1, 2, 2, 2, 1, 1, 1, 1,
                  2, 2, 2, 0, 0, 0, 2, 2, 2, 2,
                  2, 2, 2, 0, 0, 0, 2, 2, 2, 2,
                  2, 2, 2, 0, 0, 0, 2, 2, 2, 2,
                  1, 1, 1, 2, 2, 2, 1, 1, 1, 1,
                  1, 1, 1, 2, 2, 2, 1, 1, 1, 1,
                  1, 1, 1, 2, 2, 2, 1, 1, 1, 1,
                  1, 1, 1, 2, 2, 2, 1, 1, 1, 1];

    state.shapeIdx = 18;

    state.point1.set(525, 850);
    state.point2.set(290, 386);    

    this.states.push(state);

};

R.Tutorial.prototype = {

    track: function(tween)
    {
        this._tweens.push(tween);
        return tween;
    },

    clearTweens: function()
    {
        let i = this._tweens.length;
        while (i--)
        {
            if (this._tweens[i]) this._tweens[i].stop();
        }
        this._tweens.length = 0;
    },

    start: function()
    {        
        this.clearTweens();
        this.well.setState(this.states[this.step].well);
        this.finger.visible = this.circle.visible = true;
        this.tweenStep1();        
       
        game.world.bringToTop(this.circle);
        game.world.bringToTop(this.finger);
    },

    tweenStep1: function()
    {        
        let p1 = this.states[this.step].point1;
        this.finger.scale.set(1.1);
        this.finger.position.set(p1.x + 30, p1.y + 30);

        this.track(game.add.tween(this.finger).to(p1, 500, Phaser.Easing.Linear.None, true));
        this.track(game.add.tween(this.finger.scale).to({ x: 1, y: 1 }, 500, Phaser.Easing.Linear.None, true));

        //       
        this.circle.scale.set(0);
        this.circle.position.set(p1.x, p1.y);

        this.track(game.add.tween(this.circle.scale).to({ x: 1, y: 1 }, 500, Phaser.Easing.Linear.None, true)).onComplete.add(this.tweenStep2, this);
    },

    tweenStep2: function()
    {        
        let p2 = this.states[this.step].point2;
        this.track(game.add.tween(this.circle).to(p2, 1000, Phaser.Easing.Linear.None, true, 500));
        this.track(game.add.tween(this.finger).to(p2, 1000, Phaser.Easing.Linear.None, true, 500)).onComplete.add(this.tweenStep3, this);
    },

    tweenStep3: function()
    {
        this.track(game.add.tween(this.circle.scale).to({ x: 0, y: 0 }, 250, Phaser.Easing.Linear.None, true));
        this.track(game.add.tween(this.finger.scale).to({ x: 1.1, y: 1.1 }, 250, Phaser.Easing.Linear.None, true)).onComplete.add(this.tweenStep4, this);
    },

    tweenStep4: function()
    {
        let p1 = this.states[this.step].point1;
        this.track(game.add.tween(this.finger).to({ x: p1.x + 30, y: p1.y + 30 }, 500, Phaser.Easing.Linear.None, true, 500)).onComplete.add(this.tweenStep1, this);
    },

    stop: function()
    {
        this.clearTweens();
        this.finger.visible = this.circle.visible = false;
    },

    destroy: function()
    {
        this.clearTweens();
        this.circle.destroy();
        this.finger.destroy();
    }
};

R.Tutorial.prototype.constructor = R.Tutorial;