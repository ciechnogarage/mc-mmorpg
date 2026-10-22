"""Read-only editor identity preflight; no project is opened or mutated."""
import argparse
import json
from review_session import ReviewSession
p=argparse.ArgumentParser();p.add_argument('--source');p.add_argument('--apis', action='store_true');args=p.parse_args()
s=ReviewSession()
print(json.dumps(s.js('JSON.stringify({projects:ModelProject.all.map(p=>({name:p.name,uuid:p.uuid,selected:p===Project})),version:Blockbench.version})'),indent=2))
if args.source:print(json.dumps(s.bind(args.source),indent=2))

if args.apis: print(json.dumps(s.js('JSON.stringify({boneMethods:Object.getOwnPropertyNames(BoneAnimator.prototype),animationMethods:Object.getOwnPropertyNames(Animation.prototype), keyframeMethods:Object.getOwnPropertyNames(Keyframe.prototype),addKeyframe:BoneAnimator.prototype.addKeyframe.toString(),getBoneAnimator:Animation.prototype.getBoneAnimator.toString(),undo:Undo.initEdit.toString().slice(0,1800)})'),indent=2))
