"""Small independent parser for validation of Minecraft structure NBT."""
import gzip,io,struct
class Reader:
 def __init__(self,data):self.s=io.BytesIO(data)
 def n(self,fmt):return struct.unpack('>'+fmt,self.s.read(struct.calcsize('>'+fmt)))[0]
 def string(self):return self.s.read(self.n('H')).decode()
 def value(self,t):
  if t==1:return self.n('b')
  if t==2:return self.n('h')
  if t==3:return self.n('i')
  if t==4:return self.n('q')
  if t==5:return self.n('f')
  if t==6:return self.n('d')
  if t==8:return self.string()
  if t==9:
   sub=self.n('B');return [self.value(sub) for _ in range(self.n('i'))]
  if t in [7,11,12]:return [self.n({7:'b',11:'i',12:'q'}[t]) for _ in range(self.n('i'))]
  if t==10:
   result={}
   while True:
    kind=self.n('B')
    if not kind:return result
    key=self.string();result[key]=self.value(kind)
  raise ValueError(t)
def read(path):
 r=Reader(gzip.decompress(path.read_bytes()));assert r.n('B')==10;r.string();result=r.value(10);assert r.s.read()==b'';return result
