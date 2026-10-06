//Well
var Well = function(left, top, rows, cols, padding, parentState, saveLast3Moves)
{
    this.left = left;
    this.top = top;    
    this.rows = rows;
    this.cols = cols;

    this.n = rows * cols;

    //
    let grid = game.add.group();
    for(var i = 0; i < this.n; ++i)
    {
        var quad = game.add.image(0, 0, R.quad);
        quad.exists = false;        
        grid.add(quad);        
    }

    let cells = new Array(rows);

    let size = quadSize + quadPadding;

    let x = 0;
    let y = top + quadSize * 0.5;
    let c = 0;

    for(let j = 0; j < rows; j++)
    {
        x = left + quadSize * 0.5;
        cells[j] = new Array(cols);
        let cj = cells[j];
        for(let i = 0; i < cols; i++)
        {
            let q = grid.getAt(c++);
            q.anchor.setTo(0.5, 0.5);
            q.x = x;
            q.y = y;
            q.qBody = new R.Body(q);
            cj[i] = q;
            x += size;
        }
        y += size;
    }

    this.grid = grid;
    this.cells = cells;        

    this.invSizeW = 1.0 / size;
    this.invSizeH = 1.0 / size;

    this.addingCells = [];

    this.parentState = parentState;

    //
    this.saveLast3Moves = saveLast3Moves;
    this.savedRowCells = [];

    //
    this.bodies = [];
    this.combo = 0;
    this.linesThisTurn = 0;
};

//
Well.prototype = {

    tryAddShape: function(shape)
    {
        if(this.addingCells.length > 0 || shape.isNotReadyForAdding()) return false;        

        let n = shape.existsQuads;
        let wx = 0;
        let wy = 0;

        for(let i = 0; i < n; ++i)
        {
            wx = Math.floor((shape.getQuadWorldX(i) - this.left) * this.invSizeW);
            wy = Math.floor((shape.getQuadWorldY(i) - this.top) * this.invSizeH);
            
            if(wx < 0 || wx >= this.cols || wy < 0 || wy >= this.rows || this.cells[wy][wx].exists) break;            
            
            this.addingCells.push(this.cells[wy][wx]);            
        }      

        if(this.addingCells.length === n)
        {            
            shape.beginAddingToWell(this, this.addingCells[0]);
            return true;
        }
        this.addingCells.length = 0;
        return false;
    },

    onCompliteAddingShape: function(frame)
    {
        if(this.saveLast3Moves)
        {            
            if(this.savedRowCells.length > 2) this.savedRowCells.shift();
            this.savedRowCells.push(this.getRowCells());
        }
        
        //
        let i = this.addingCells.length;
        while(i--)
        {
            this.addingCells[i].exists = true;
            if (typeof frame === 'number') this.addingCells[i].tint = frame;
        }
        
        this.addingCells.length = 0;
        this.linesThisTurn = 0;
        this.checkLines();
        if (this.linesThisTurn > 0)
        {
            this.combo = (this.combo || 0) + 1;
            if (this.parentState && this.parentState.onLinesCleared) this.parentState.onLinesCleared(this.linesThisTurn, this.combo);
        }
        else
        {
            this.combo = 0;
            if (this.parentState && this.parentState.labelCombo) this.parentState.labelCombo.text = '';
        }
        this.parentState.onCompleteAddingShapeToWell();        
    },

    collectTargets: function(shape)
    {
        if (!shape || shape.isNotReadyForAdding()) return null;

        let n = shape.existsQuads;
        let targets = [];
        let wx = 0;
        let wy = 0;

        for (let i = 0; i < n; ++i)
        {
            wx = Math.floor((shape.getQuadWorldX(i) - this.left) * this.invSizeW);
            wy = Math.floor((shape.getQuadWorldY(i) - this.top) * this.invSizeH);
            if (wx < 0 || wx >= this.cols || wy < 0 || wy >= this.rows || this.cells[wy][wx].exists) return null;
            targets.push(this.cells[wy][wx]);
        }

        return targets.length === n ? targets : null;
    },

    checkLines: function()
    {
        let i = 0;
        let j = this.rows;
        let row = null;

        while(j--)
        {
            row = this.cells[j];
            i = this.cols;
            while(i--) if(!row[i].exists) break;
            if(i === -1) this.removeRow(j);
        }

        j = this.cols;
        while(j--)
        {
            i = this.rows;
            while(i--) if(!this.cells[i][j].exists) break;
            if(i === -1) this.removeCol(j);
        }

    },

    removeRow: function(j)
    {
        let row = this.cells[j];
        let i = row.length;
        while(i--)
        {
            row[i].qBody.launch();
            this.bodies.push(row[i].qBody);            
        }
        game.world.bringToTop(this.grid);
        R.score += this.cols;
        this.linesThisTurn = (this.linesThisTurn || 0) + 1;
        if(R.sfx.row_removed) R.sfx.row_removed.play();
    },

    removeCol: function(j)
    {     
        let i = this.rows;
        while(i--)
        {
            this.cells[i][j].qBody.launch();
            this.bodies.push(this.cells[i][j].qBody);            
        }
        game.world.bringToTop(this.grid);
        R.score += this.rows;
        this.linesThisTurn = (this.linesThisTurn || 0) + 1;
        if(R.sfx.row_removed) R.sfx.row_removed.play();
    },

    findShapePlace: function(state)
    {   
        let ci = 0;
        let si = 0;
        let sj = 0;
        let ci2 = 0;
        let cj2 = 0;

        let lj = this.cols - state.length + 1;
        let li = this.rows - state[0].length + 1;

        let isNextCells = false;

        for(let cj = 0; cj < lj; ++cj)
        {            
            for(ci = 0; ci < li; ++ci)
            {                                
                for(sj = 0, cj2 = cj; sj < state.length; ++sj, ++cj2)
                {                    
                    for(si = 0, ci2 = ci; si < state[sj].length; ++si, ++ci2)
                    {
                        if(state[sj][si] === 1 && this.cells[cj2][ci2].exists)
                        {
                            isNextCells = true;
                            break;
                        }                        
                    }
                    if(isNextCells) break;
                }
                if(isNextCells) isNextCells = false;
                else return true;
            }
        }

        return false;
    },

    getRowCells: function()
    {
        let c = 0;
        let rowCells = [];
        for(let r = 0; r < 10; ++r)
        {
            let row = [];
            for(c = 0; c < 10; ++c) row.push(this.cells[r][c].exists ? this.cells[r][c].frame : -1);            
            rowCells.push(row);
        }        
        return rowCells;
    },

    remove3PastMoves: function()
    {
        let rowCells = this.savedRowCells[0];
        console.log(JSON.stringify(rowCells));
        let r = rowCells.length;
        let c = 0;
        while(r--)
        {
            c = rowCells[r].length;
            while(c--)
            {
                if(this.cells[r][c].exists = (rowCells[r][c] !== -1)) this.cells[r][c].frame = rowCells[r][c];
            }
        }

        //this.savedRowCells.length = 0;
        //this.saveLast3Moves = false;
    },

    update: function()
    {
        let removingQuads = this.bodies.length > 0;
        if(removingQuads)
        {
            let i = 0;
            while(i < this.bodies.length)
            {
                if(this.bodies[i].update()) ++i;
                else this.bodies.splice(i, 1);
            }

            if(this.bodies.length === 0) this.parentState.onComleteRemoveLines();            
        }
    },

    setState: function(array)
    {
        for(let r = 0; r < this.rows; ++r)
        {
            for(let c = 0; c < this.cols; ++c)
            {
                let s = array[r * this.cols + c];
                this.cells[r][c].exists = s !== 0;
                this.cells[r][c].visible = s === 2;
            }
        }
    },

    reset: function()
    {
        for(let r = 0; r < this.rows; ++r)
        {
            for(let c = 0; c < this.cols; ++c)
            {                                
                this.cells[r][c].visible = true;
                this.cells[r][c].exists = false;
            }
        }
    }
};

//
Well.prototype.constructor = Well;

//Body
R.Body = function(quad)
{
    this.quad = quad;
    this.wellPosition = { x: quad.position.x, y: quad.position.y };
    this.velocity = new Phaser.Point(0, 0);    
    this.torque = 0;
};

R.Body.prototype = {

    launch: function()
    {
        this.quad.parent.bringToTop(this.quad);
        this.quad.scale.set(1.2);
        this.velocity.x = game.rnd.realInRange(-2, 2);
        this.velocity.y = -game.rnd.realInRange(6, 10);
        this.torque = game.rnd.realInRange(-0.2, 0.2);
    },

    update: function()
    {
        if(this.quad.position.y < R.BASE_GAME_HEIGHT + quadSize)
        {
            this.quad.position.x += this.velocity.x;
            this.quad.position.y += this.velocity.y;
            this.quad.rotation += this.torque;
            if(this.quad.scale.x > 0.02) this.quad.scale.set(this.quad.scale.x - 0.015);            
            this.velocity.y += 0.6;
            return true;
        }

        this.quad.position.x = this.wellPosition.x;
        this.quad.position.y = this.wellPosition.y;
        this.quad.rotation = 0;
        this.quad.scale.set(1);
        this.quad.exists = false;

        return false;
    }
};

R.Body.prototype.constructor = R.Body;