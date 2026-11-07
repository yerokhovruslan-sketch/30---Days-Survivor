from pathlib import Path
import re, collections
root=Path(r'D:/AndroidProjects')
s=(root/'app/src/main/java/com/example/survivor/game/Item.java').read_text(encoding='utf-8')
rows=[]
pat=r'items\.add\(new (Food|Tools|Weapons)\("([^"]+)",\s*"[^"]*",\s*(\d+),\s*(\d+)'
for typ,name,rar,price in re.findall(pat,s): rows.append((name,int(rar),int(price),False))
for name,heal,price in re.findall(r'items\.add\(new MedicalItem\("([^"]+)",\s*(\d+),\s*(\d+)\)',s):rows.append((name,1,int(price),True))
ammo=[('Дробовой патрон ×6',3,144),('Револьверный патрон ×6',3,96),('Арбалетный болт ×5',2,45),('Шарик для рогатки ×12',1,6),('Пистолетный патрон ×12',3,132),('Карабинный патрон ×5',4,130)]
rows += [(n,r,p,False) for n,r,p in ammo]
rows += [(n,r,p,False) for n,r,p in [('Порох',3,8),('Комплект деталей для патронов',3,12),('Металл',1,5),('Древесина',1,5),('Пустая револьверная гильза',1,3),('Пустая пистолетная гильза',1,2),('Пустая дробовая гильза',1,3),('Пустая карабинная гильза',1,4)]]
counts=collections.Counter(r for n,r,p,m in rows)
print('COUNT',len(rows),dict(counts))
weights={1:[70,55,40],2:[10,10,10],3:[10,10,10],4:[6,6,6],5:[4,19,34]}
def odds(r,d):return .85*weights[r][d-1]/counts[r]
lines=['# Прайс Survivor — текущий код','', 'Цены находок в обычных/средних/сложных локациях: базовая ×1/×2/×3. Медицинские предметы не умножаются. Цена продажи обычных находок равна их цене; боеприпасы продаются за половину. Состояние прочности само по себе цену не снижает.', '', '## Весь пул случайных находок','', '| Предмет | Редкость | Цена ×1 | Цена ×2 | Цена ×3 | Шанс при успешной находке, GENERAL: ×1 / ×2 / ×3 |','|---|---:|---:|---:|---:|---|']
for n,r,p,m in sorted(rows,key=lambda x:(-x[2],x[0])):
    # ammo ignores multiplyPrice in getPrice override
    isammo=any(n==a[0] for a in ammo)
    prices=[p if m or isammo else p*d for d in [1,2,3]]
    prob=' / '.join(f'{odds(r,d):.4f}%' for d in [1,2,3])
    if m:prob='12% отдельная медицинская ветка + доля в общем пуле'
    lines.append(f'| {n} | {r} | {prices[0]} | {prices[1]} | {prices[2]} | {prob} |')
lines+=['','Проценты относятся к GENERAL, после успешного поиска: 15% отдельная медицина, остальные 85% — таблица редкости и равномерный выбор внутри редкости. В тематических локациях 65% выборов идёт из подходящего пула, поэтому индивидуальные проценты меняются. Шанс за нажатие «искать» дополнительно умножается на текущий шанс добычи локации и зависит от режима поиска. Сюжетные и вражеские трофеи рассчитываются отдельно.','', '## Находки с ценой не выше 900 $ в сложной локации', '', '| Предмет | Редкость | Цена ×3 |', '|---|---:|---:|']
for n,r,p,m in sorted(rows,key=lambda x:-x[2]):
    value=p if m or any(n==a[0] for a in ammo) else p*3
    if value<=900:lines.append(f'| {n} | {r} | {value} |')
shop=(root/'app/src/main/java/com/example/survivor/game/Shop.java').read_text(encoding='utf-8')
store={}
for num,typ,n,r,p in re.findall(r'case (\d+):\s*boughtItem\s*=\s*new (Weapons|Tools|Food)\("([^"]+)",\s*"[^"]*",\s*(\d+),\s*(\d+)',shop):store[int(num)]=(n,int(r),int(p))
for num,n,h,p in re.findall(r'case (\d+):\s*boughtItem\s*=\s*new MedicalItem\("([^"]+)",\s*(\d+),\s*(\d+)\)',shop):store[int(num)]=(n,1,int(p))
for num,n,r,p in [(12,'Дробовые патроны ×4',3,96),(13,'Револьверные патроны ×4',3,64),(14,'Арбалетные болты ×4',2,36),(15,'Шарики для рогатки ×12',1,6),(52,'Пистолетные патроны ×6',3,66),(53,'Карабинные патроны ×4',4,104)]:store[num]=(n,r,p)
for num,row in zip(range(54,62), [('Порох',3,8),('Пустая револьверная гильза',1,3),('Пустая пистолетная гильза',1,2),('Пустая дробовая гильза',1,3),('Пустая карабинная гильза',1,4),('Комплект деталей для патронов',3,12),('Металл',1,5),('Древесина',1,5)]):store[num]=row
lines+=['','## Весь магазин','', '| № | Предмет | Редкость | Покупка, $ |','|---:|---|---:|---:|']
for num,(n,r,p) in sorted(store.items()):lines.append(f'| {num} | {n} | {r} | {p} |')
lines+=['','Электрочип: 1200 $ за штуку, в случайном пуле отсутствует (шанс обычной случайной находки 0%). Порох: 8 шт. в нечётные игровые дни. Комплекты деталей: 3 шт. в дни 1, 4, 7 и т.д. Остальные товары без лимита по остаткам; действуют деньги и вместимость.', '', '## Только крафт (цена результата / продажа / стоимость компонентов)', '', 'Топор: 40 / 10 / 59 $. Мачете: 145 / 36 / 67 $. Катана: 220 / 55 / 102 $. Пожарный топор: 145 / 36 / 117 $. Это именно изготовленные варианты; в пуле находок пока также остались старые топор, мачете и пожарный топор с другими ценами. Катаны в случайном пуле нет.']
out=root/'output/survivor-price-list.md';out.parent.mkdir(exist_ok=True);out.write_text('\n'.join(lines),encoding='utf-8')
print('SHOP',len(store))
for row in sorted(rows,key=lambda x:-x[2])[:15]:print(row,'odds',odds(row[1],1),odds(row[1],3))
