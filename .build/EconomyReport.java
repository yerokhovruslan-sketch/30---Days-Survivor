package com.example.survivor.game;
import java.nio.file.*;
public class EconomyReport {
 public static void main(String[] args)throws Exception {
  StringBuilder out=new StringBuilder("# Прайс Survivor — экономика 2.0\n\nЕдиные базовые цены без множителя сложности. Обычная скупка: 50% при прочности 100, 35% при 50, 20% при 0. Недолговечные предметы 50%, боеприпасы 40%. События меняют цены покупки и спрос на инструменты.\n\n## Магазин (базовые цены без события)\n\n| Предмет | Редкость | Покупка | Продажа нового |\n|---|---:|---:|---:|\n");
  for(int n=1;n<=61;n++){ItemInfo i=Shop.createItem(n);if(i!=null)out.append("| ").append(i.getName()).append(i instanceof Ammunition?" ×"+((Ammunition)i).getQuantity():"").append(" | ").append(i.getRarity()).append(" | ").append(i.getPrice()).append(" | ").append(i.getSalePrice()).append(" |\n");}
  out.append("\n## Все записи случайного пула\n\n| Предмет | Редкость | Базовая оценка | Продажа при прочности 100 |\n|---|---:|---:|---:|\n");
  for(ItemInfo i:Item.lootCatalogue())out.append("| ").append(i.getName()).append(i instanceof Ammunition?" ×"+((Ammunition)i).getQuantity():"").append(" | ").append(i.getRarity()).append(" | ").append(i.getPrice()).append(" | ").append(i.getSalePrice()).append(" |\n");
  out.append("\n## Изготовление\n\n| Рецепт | Количество | Компоненты, базовая покупная стоимость | Продажа результата |\n|---|---:|---:|---:|\n");
  for(Crafting.Recipe r:Crafting.RECIPES)out.append("| ").append(r.name).append(" | ").append(r.amount).append(" | ").append(r.cost(1)).append(" | ").append(Crafting.output(r,1).stream().mapToInt(ItemInfo::getSalePrice).sum()).append(" |\n");
  Files.writeString(Path.of("output/survivor-price-list.md"),out.toString());
 }
}
