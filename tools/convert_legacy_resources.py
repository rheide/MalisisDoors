"""Convert legacy recipe/translation data; only emit recipes for implemented outputs."""
from pathlib import Path
import json,re
R=Path(__file__).resolve().parents[1];source=R.parent/'legacy/src/main/resources/assets/malisisdoors';dest=R/'src/main/resources'
woods=['oak','spruce','birch','jungle','acacia','dark_oak'];colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
implemented={p.stem for p in (dest/'assets/malisisdoors/models/item').glob('*.json')}
def modern_id(item):
 item=item.lower()
 return {'minecraft:wooden_door':'minecraft:oak_door','minecraft:fence_gate':'minecraft:oak_fence_gate','minecraft:trapdoor':'minecraft:oak_trapdoor'}.get(item,item)
def ingredient(value):
 if value.get('ore')=='blockGlassColorless':return {'tag':'c:glass_blocks/colorless'}
 item=value['item'];data=value.get('data',0)
 if item=='minecraft:planks':return {'tag':'minecraft:planks'} if data==32767 else {'item':'minecraft:'+woods[data]+'_planks'}
 if item=='minecraft:wool':return {'item':'minecraft:'+colors[data]+'_wool'}
 return {'item':modern_id(item)}
count=0
for path in (source/'recipes').glob('*.json'):
 data=json.loads(path.read_text());output=data.get('result',{}).get('item')
 if output is None or modern_id(output).split(':')[-1] not in implemented or data.get('type')=='malisisdoors:big_door':continue
 if 'key' in data:data['key']={key:ingredient(value) for key,value in data['key'].items()}
 if 'ingredients' in data:data['ingredients']=[ingredient(value) for value in data['ingredients']]
 data['result']['id']=modern_id(data['result'].pop('item'))
 target=dest/'data/malisisdoors/recipe'/path.name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(data,indent=2)+'\n');count+=1
for path in (source/'lang').glob('*.lang'):
 try:content=path.read_text(encoding='utf-8')
 except UnicodeDecodeError:content=path.read_text(encoding='cp1252')
 entries={}
 for line in content.splitlines():
  if not line or line.startswith('#') or '=' not in line:continue
  key,value=line.split('=',1);entries[key]=value.replace('\\n','\n')
  match=re.match(r'(tile|item)\.(?:malisisdoors\.)?(.+)\.name$',key)
  if match:
   kind,id=match.groups();id=id.lower();kind='block' if kind=='tile' else 'item';entries[f'{kind}.malisisdoors.{id}']=value
 for item in implemented:entries.setdefault('block.malisisdoors.'+item,item.replace('_',' ').title())
 target=dest/'assets/malisisdoors/lang'/(path.stem+'.json');target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(entries,indent=2,ensure_ascii=False)+'\n')
print('Converted',count,'implemented recipes and legacy translations')
