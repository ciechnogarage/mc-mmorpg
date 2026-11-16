"""Shared read-only asset review session. Never opens or edits a project."""
import hashlib
import json
from pathlib import Path
from mcp_client import call
from project_identity import fingerprint, require_match


def literal(value):
    return json.dumps(value).replace('/', '\\u002f')


def decode_export(result):
    try:
        if result.get('isError'):
            raise ValueError('MCP export failed')
        text = next(c['text'] for c in result['content'] if c['type'] == 'text')
        envelope = json.loads(text)
        if envelope.get('truncated', True):
            raise ValueError('Export missing completeness flag or truncated')
        model = json.loads(envelope['content'])
        if not isinstance(model, dict) or not {'elements','groups','animations','textures'} <= model.keys():
            raise ValueError('Export is not a complete bbmodel')
        return model
    except (KeyError, StopIteration, TypeError, json.JSONDecodeError) as exc:
        raise ValueError('Malformed MCP export') from exc


def isolate_animation_js(name_expression):
    """Select one clip without allowing locked animations to contaminate preview."""
    return ('const a=Animation.all.find(a=>a.name===' + name_expression + '); '
            'if(!a)throw new Error("Missing animation"); Timeline.pause(); '
            'AnimationItem.all.forEach(item=>{item.playing=false;}); '
            'Modes.options.animate.select(); a.select(); '
            'AnimationItem.all.forEach(item=>{item.playing=false;}); a.playing=true; ')


class ReviewSession:
    def __init__(self):
        response = call('initialize', {'protocolVersion':'2025-03-26','capabilities':{},'clientInfo':{'name':'undead-verified-review','version':'2'}})
        if not response or 'error' in response:
            raise RuntimeError('Cannot initialize Blockbench MCP')
        self.server_info = response['result'].get('serverInfo')
        call('notifications/initialized', notification=True)

    def tool(self, name, args):
        response = call('tools/call', {'name':name,'arguments':args})
        if not response or 'error' in response or response.get('result', {}).get('isError'):
            raise RuntimeError(str(response))
        result = response['result']
        for part in result.get('content', []):
            if part.get('type') == 'text' and part['text'].startswith('Error'):
                raise RuntimeError(part['text'])
        return result

    def js(self, code):
        result = self.tool('risky_eval', {'code':code})
        text = next(c['text'] for c in result['content'] if c['type']=='text')
        value = json.loads(text)
        return json.loads(value) if isinstance(value, str) else value

    def export(self):
        return decode_export(self.tool('export_model', {'codec_id':'project','max_content_length':2000000}))

    def bind(self, path):
        path = Path(path)
        payload = path.read_bytes()
        expected = json.loads(payload)
        actual = self.export()
        require_match(expected, actual)
        self.expected = expected
        return {'source':str(path.resolve()), 'source_sha256':hashlib.sha256(payload).hexdigest(),
                'project_fingerprint':fingerprint(actual), 'mcp':self.server_info}

    def snapshot(self):
        return self.js('JSON.stringify({project:Project.uuid, mode:Modes.selected.id, animation:Animation.selected ? Animation.selected.uuid : null, selected_item:AnimationItem.selected ? AnimationItem.selected.uuid : null, items:AnimationItem.all.map(a=>({uuid:a.uuid,playing:a.playing})), timeline_playing:Timeline.playing, time:Timeline.time})')

    def restore(self, state):
        self.js('const s='+literal(state)+'; const p=ModelProject.all.find(p=>p.uuid===s.project); '
                'if(!p)throw new Error("Original project no longer exists"); Timeline.pause(); p.select(); '
                'Modes.options[s.mode].select(); '
                'const selected=AnimationItem.all.find(item=>item.uuid===s.selected_item); '
                'if(s.selected_item && !selected)throw new Error("Original animation item no longer exists"); '
                'if(selected){selected.select();}else{AnimationItem.all.forEach(item=>{item.selected=false;}); AnimationItem.selected=null; Timeline.clear();} '
                'AnimationItem.all.forEach(item=>{item.playing=false;}); '
                'for(const saved of s.items){const item=AnimationItem.all.find(item=>item.uuid===saved.uuid); '
                'if(!item)throw new Error("Original animation stack item no longer exists"); item.playing=saved.playing;} '
                'Timeline.setTime(s.time); if(s.mode==="animate")Animator.preview(); '
                'if(s.timeline_playing && s.mode==="animate")Timeline.start(); JSON.stringify({restored:true})')

    def pose(self, name, time):
        self.js(isolate_animation_js(literal(name)) + 'Timeline.setTime('+str(float(time))+'); Animator.preview(); JSON.stringify({posed:true})')
