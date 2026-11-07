from pathlib import Path
p=Path('app/src/main/java/com/example/survivor/TapFeedback.java');s=p.read_text(encoding='utf-8').replace('static void run(', 'static boolean run(').replace('if(pending || !view.isEnabled())return;', 'if(pending || !view.isEnabled())return false;').replace('        },300);','        },300);return true;');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/MenuRenderer.java');s=p.read_text(encoding='utf-8').replace('    private boolean wasBoard;', '    private BattleBoardView boardView;\n    private boolean wasBoard;')
s=s.replace('        if (frame.menu.board != null) append(cards, new BattleBoardView(context, frame.menu.board, number->{listener.click();listener.action(number);}), 10);', '''        if (frame.menu.board != null){
            if(boardView==null)boardView=new BattleBoardView(context,frame.menu.board,number->{listener.click();listener.action(number);});
            else boardView.update(frame.menu.board);
            append(cards,boardView,10);
        }''')
s=s.replace('body.setOnClickListener(view -> {listener.click();listener.action(enemy.number);});', 'body.setOnClickListener(view -> {if(TapFeedback.run(view,()->listener.action(enemy.number)))listener.click();});')
s=s.replace('row.setOnClickListener(v->{listener.click();listener.action(choice.number);});', 'row.setOnClickListener(v->{if(TapFeedback.run(v,()->listener.action(choice.number)))listener.click();});')
s=s.replace('button.setOnClickListener(view -> {listener.click();action.run();});', 'button.setOnClickListener(view -> {if(TapFeedback.run(view,action))listener.click();});')
p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/MainActivity.java');s=p.read_text(encoding='utf-8')
s=s.replace('view->{game.clickSound();game.continueRadio();}', 'view->{if(TapFeedback.run(view,()->game.continueRadio()))game.clickSound();}')
s=s.replace('view -> {game.clickSound();game.continueIntro();}', 'view -> {if(TapFeedback.run(view,()->game.continueIntro()))game.clickSound();}')
s=s.replace('                game.clickSound();\n                game.section = button.getText().toString();\n                submit(action);', '''                GameSession.Frame expected=frame;
                if(TapFeedback.run(view,()->{if(frame!=expected)return;game.section=button.getText().toString();submit(action);}))game.clickSound();''')
p.write_text(s,encoding='utf-8')
