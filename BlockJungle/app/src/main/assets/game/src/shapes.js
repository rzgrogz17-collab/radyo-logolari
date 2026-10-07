//ShapeStates
let ShapeStates = function()
{    
    this.state = [];
    
    //
    //1
    this.state[0] = [[1]];

    //2
    this.state[1] = [[1, 1]];

    this.state[2] = [[1],
                     [1]];

    //3
    this.state[3] = [[1, 1, 1]];

    this.state[4] = [[1],
                     [1],
                     [1]];
    
    this.state[5] = [[1, 1],
                     [0, 1]];

    this.state[6] = [[1, 1],
                     [1, 0]];

    this.state[7] = [[1, 0],
                     [1, 1]];

    this.state[8] = [[0, 1],
                     [1, 1]];

    //4
    this.state[9] = [[1, 1, 1, 1]];

    this.state[10] = [[1],
                      [1],
                      [1],
                      [1]];

    this.state[11] = [[1, 1],
                      [1, 1]];

    //5
    this.state[12] = [[1, 1, 1, 1, 1]];

    this.state[13] = [[1],
                      [1],
                      [1],
                      [1],
                      [1]];
   
    this.state[14] = [[1, 0, 0],
                      [1, 0, 0],
                      [1, 1, 1]];

    this.state[15] = [[1, 1, 1],
                      [1, 0, 0],
                      [1, 0, 0]];

    this.state[16] = [[0, 0, 1],
                      [0, 0, 1],
                      [1, 1, 1]];

    this.state[17] = [[1, 1, 1],
                      [0, 0, 1],
                      [0, 0, 1]];
    //9
    this.state[18] = [[1, 1, 1],
                      [1, 1, 1],
                      [1, 1, 1]];

    // Extra pieces. Tutorial still uses indexes 3, 4 and 18.
    this.state[19] = [[1, 1, 1],
                      [0, 1, 0]];
    this.state[20] = [[0, 1, 0],
                      [1, 1, 1]];
    this.state[21] = [[1, 0],
                      [1, 1],
                      [1, 0]];
    this.state[22] = [[0, 1],
                      [1, 1],
                      [0, 1]];
    this.state[23] = [[1, 0],
                      [1, 0],
                      [1, 1]];
    this.state[24] = [[1, 1, 1],
                      [1, 0, 0]];
    this.state[25] = [[0, 1],
                      [0, 1],
                      [1, 1]];
    this.state[26] = [[1, 0, 0],
                      [1, 1, 1]];
    this.state[27] = [[0, 1, 1],
                      [1, 1, 0]];
    this.state[28] = [[1, 1, 0],
                      [0, 1, 1]];
    this.state[29] = [[1, 0],
                      [1, 1],
                      [0, 1]];
    this.state[30] = [[0, 1],
                      [1, 1],
                      [1, 0]];

};

//
ShapeStates.prototype = {

    getRnd: function()
    {
        // Keep the original tray most of the time, and mix in the new pieces.
        let extra = this.state.length - 19;
        if (extra > 0 && game.rnd.integerInRange(0, 9) < 3)
        {
            return this.state[19 + game.rnd.integerInRange(0, extra - 1)];
        }
        return this.state[game.rnd.integerInRange(0, 18)];
    }
};

//
ShapeStates.prototype.constructor = ShapeStates;

//QuadShadow
R.shd = 8;

//
R.shadowCaster = {

    shadows: [],

    update: function()
    {
        let i = this.shadows.length;
        let sh = null;
        
        while(i--)
        {
            sh = this.shadows[i];
            if(sh.exists = (sh.quad.exists && sh.shape.padding.x >= quadPadding)) this.updateShadow(sh);
        }
    },

    updateShadow: function(sh)
    {
        let shd = R.shd;//R.shd * (sh.shape.padding.x / quadPaddingDrag);
        if(sh.shape.addingToWell) shd *= sh.shape.padding.x / quadPaddingDrag;

        sh.x = sh.quad.x + shd;
        sh.y = sh.quad.y + shd;
    },

    clear: function()
    {
        this.shadows.length = 0;
    },

    add: function(sh)
    {
        this.shadows.push(sh);
    }
};

//
R.QuadShadow = function(quad, shape)
{
    Phaser.Image.call(this, game, 0, 0, 'quad_shadow');
    this.anchor.set(0.5);
    this.quad = quad;
    this.shape = shape;
    this.exists = false;   
    R.shadowCaster.add(this);
};

R.QuadShadow.prototype = Object.create(Phaser.Image.prototype);
R.QuadShadow.prototype.constructor = R.QuadShadow;

//Shape
R.quad = 'quad_0';
const quadSize = 58;
const quadPadding = 0;
const quadScaleMin = 0.6;
const quadScaleDrag = 0.85;
const quadPaddingDrag = (quadSize + quadPadding) / quadScaleDrag - quadSize;//10.235294117647063;

//
let Shape = function(nQuads, startX, startY)
{    
    this.state = null;
    this.startX = startX;
    this.startY = startY;
    this.padding = new Phaser.Point(0, 0);
    this.hh = 0;
    this.addingToWell = false;

    this.parent = game.add.image(startX, startY, null);

    this.quads = new Array(nQuads);

    for(let i = 0; i < nQuads; i++)
    {        
        this.quads[i] = game.add.image(0, 0, R.quad);
        this.quads[i].anchor.set(0.5);
        this.parent.addChild(new R.QuadShadow(this.quads[i], this));
        this.parent.addChild(this.quads[i]);
    }

    this.existsQuads = nQuads;   
    this.well = null;

    //
    this.tweenDragScale = null;
    this.tweenDragPadding = null;
    this.tweenEndDragPosition = null;
    this.tweenEndDragScale = null;

    //
    this.readyForDrag = true;
};

//
Shape.prototype = {
    
    isExists: function()
    {
        return this.parent.exists;
    },

    setState: function(state)
    {
        this.state = state;
        this.tintColor = R.tintFor ? R.tintFor(state) : 0xffc56a;

        let i = this.quads.length;
        while(i--) this.quads[i].exists = false;
        
        this.buildShape(quadPadding);      

        this.parent.scale.set(quadScaleMin);
        this.parent.exists = true;
        this.padding.x = quadPadding;
    },

    setPosition: function(x, y)
    {
        this.parent.x = x;
        this.parent.y = y;
    },

    buildShape: function(padding)
    {
        let row = null;
        let i = 0;
        let idxQuad = 0;
        let size = quadSize + padding;
        let x = 0;
        let y = 0;
        let q = null;
        let maxX = 0;
        let maxY = 0;

        this.existsQuads = 0;

        for(let j = 0; j < this.state.length; ++j)
        {
            row = this.state[j];
            x = 0;
            for(i = 0; i < row.length; ++i)
            {
                if(row[i] === 1)
                {
                    q = this.quads[idxQuad];
                    q.x = x;
                    q.y = y;
                    q.exists = true;
                    q.tint = this.tintColor || 0xffffff;
                    if(maxX < x) maxX = x;
                    if(maxY < y) maxY = y;
                    ++idxQuad;
                }                
                x += size;
            }
            y += size;
        }

        this.existsQuads = idxQuad;

        //
        maxX *= 0.5;
        maxY *= 0.5;
        i = this.quads.length;

        while(i--)
        {
            if(this.quads[i].exists)
            {
                this.quads[i].x -= maxX;
                this.quads[i].y -= maxY;
            }
        }

        this.hh = maxY + size + 70;
    },

    updatePadding: function()
    {
        this.buildShape(this.padding.x);
    },

    startDrag: function()
    {
        this.readyForDrag = false;

        if(this.tweenEndDragPosition && this.tweenEndDragPosition.isRunning) this.tweenEndDragPosition.stop(true);
        if(this.tweenEndDragScale && this.tweenEndDragScale.isRunning) this.tweenEndDragScale.stop(true);

        this.tweenDragScale = game.add.tween(this.parent.scale).to({ x: quadScaleDrag, y: quadScaleDrag }, 100, Phaser.Easing.Linear.None, true);
        this.tweenDragScale.onComplete.add(function() { this.tweenDragScale = null; }, this);

        this.tweenDragPadding = game.add.tween(this.padding).to({ x: quadPaddingDrag }, 100, Phaser.Easing.Linear.None, true);
        this.tweenDragPadding.onUpdateCallback(this.updatePadding, this);        
        this.tweenDragPadding.onComplete.add(function() { this.updatePadding(); this.tweenDragPadding = null; }, this);
    },

    isNotReadyForAdding: function()
    {
        return (this.tweenDragScale && this.tweenDragScale.isRunning) || (this.tweenDragPadding && this.tweenDragPadding.isRunning);        
    },

    endDrag: function()
    {
        if(this.tweenDragScale && this.tweenDragScale.isRunning) this.tweenDragScale.stop(true);
        if(this.tweenDragPadding && this.tweenDragPadding.isRunning) this.tweenDragPadding.stop(true); 

        this.tweenDragPadding = game.add.tween(this.padding).to({ x: quadPadding }, 160, Phaser.Easing.Linear.None, true);
        this.tweenDragPadding.onUpdateCallback(this.updatePadding, this);
        this.tweenDragPadding.onComplete.add(function() { this.updatePadding(); this.tweenDragPadding = null; }, this);

        this.tweenEndDragPosition = game.add.tween(this.parent).to({ x: this.startX, y: this.startY }, 160, Phaser.Easing.Linear.None, true);
        this.tweenEndDragPosition.onComplete.add(function() { this.onReadyForDrag(); this.tweenEndDragPosition = null; }, this);

        this.tweenEndDragScale = game.add.tween(this.parent.scale).to({ x: quadScaleMin, y: quadScaleMin }, 160, Phaser.Easing.Linear.None, true);
        this.tweenEndDragScale.onComplete.add(function() { this.tweenEndDragScale = null; }, this);
    },

    getQuadWorldX: function(i)
    {
        return this.parent.x + this.quads[i].x * this.parent.scale.x;
    },

    getQuadWorldY: function(i)
    {
        return this.parent.y + this.quads[i].y * this.parent.scale.x;
    },

    beginAddingToWell: function(well, firstAddingCell)
    {
        if(this.tweenDragScale && this.tweenDragScale.isRunning) this.tweenDragScale.stop(true);
        if(this.tweenDragPadding && this.tweenDragPadding.isRunning) this.tweenDragPadding.stop(true);

        let tx = firstAddingCell.x - this.getQuadWorldX(0) + this.parent.x + well.grid.x;
        let ty = firstAddingCell.y - this.getQuadWorldY(0) + this.parent.y + well.grid.y;
        this.well = well;

        let tween = game.add.tween(this.padding).to({ x: quadPadding }, 100, Phaser.Easing.Linear.None, true);
        tween.onUpdateCallback(this.updatePadding, this);
        tween.onComplete.add(this.updatePadding, this);

        this.tweenDragScale = game.add.tween(this.parent.scale).to({ x: 1.0, y: 1.0 }, 100, Phaser.Easing.Linear.None, true);
        this.tweenDragScale.onComplete.add(function() { this.tweenDragScale = null; }, this);

        tween = game.add.tween(this.parent).to({ x: tx, y: ty }, 100, Phaser.Easing.Linear.None, true);
        tween.onComplete.add(this.onCompleteAddingToWell, this);

        //
        this.addingToWell = true;
    },

    onCompleteAddingToWell: function()
    {
        if(this.tweenDragScale && this.tweenDragScale.isRunning) this.tweenDragScale.stop(true);
        this.parent.exists = false;
        R.score += this.existsQuads;
        this.well.onCompliteAddingShape(this.tintColor || 0xffffff);

        //
        this.addingToWell = false;
    },

    reset: function(state)
    {        
        this.setPosition(this.startX + game.width, this.startY);
        this.setState(state);

        let tween = game.add.tween(this.parent).to({ x: this.startX }, 300, Phaser.Easing.Quadratic.Out, true);
        tween.onComplete.add(this.onReadyForDrag, this);
    },

    onReadyForDrag: function()
    {
        this.readyForDrag = true;
    }
};

//
Shape.prototype.constructor = Shape;