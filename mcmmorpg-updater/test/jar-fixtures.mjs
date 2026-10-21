export function buildJar(entries, { declaredUncompressedSize } = {}) {
  const locals = [];
  const central = [];
  let offset = 0;

  for (const [index, entry] of entries.entries()) {
    const name = Buffer.from(entry.name);
    const body = Buffer.from(entry.body ?? '');
    const uncompressedSize = index === 0 && declaredUncompressedSize !== undefined
      ? declaredUncompressedSize
      : body.length;
    const local = Buffer.alloc(30 + name.length + body.length);
    local.writeUInt32LE(0x04034b50, 0);
    local.writeUInt16LE(20, 4);
    local.writeUInt16LE(0, 6);
    local.writeUInt16LE(0, 8);
    local.writeUInt32LE(0, 14);
    local.writeUInt32LE(body.length, 18);
    local.writeUInt32LE(uncompressedSize, 22);
    local.writeUInt16LE(name.length, 26);
    name.copy(local, 30);
    body.copy(local, 30 + name.length);
    locals.push(local);

    const directory = Buffer.alloc(46 + name.length);
    directory.writeUInt32LE(0x02014b50, 0);
    directory.writeUInt16LE(20, 4);
    directory.writeUInt16LE(20, 6);
    directory.writeUInt16LE(0, 8);
    directory.writeUInt16LE(0, 10);
    directory.writeUInt32LE(0, 16);
    directory.writeUInt32LE(body.length, 20);
    directory.writeUInt32LE(uncompressedSize, 24);
    directory.writeUInt16LE(name.length, 28);
    directory.writeUInt32LE(0, 38);
    directory.writeUInt32LE(offset, 42);
    name.copy(directory, 46);
    central.push(directory);
    offset += local.length;
  }

  const centralBytes = Buffer.concat(central);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(entries.length, 8);
  end.writeUInt16LE(entries.length, 10);
  end.writeUInt32LE(centralBytes.length, 12);
  end.writeUInt32LE(offset, 16);
  return Buffer.concat([...locals, centralBytes, end]);
}

export function validExampleJar(version = '2.0.0') {
  return buildJar([
    { name: 'plugin.yml', body: `name: ExamplePlugin\nversion: ${version}\nmain: acme.Example\n` },
    { name: 'META-INF/MANIFEST.MF', body: 'Manifest-Version: 1.0\n' },
  ]);
}
