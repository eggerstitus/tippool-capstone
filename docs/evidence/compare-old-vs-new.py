import math, itertools, json

PCTS = [100, 75, 50, 25]

def old(pool, counts):
    # exact copy of TipPoolViewModel.calculate(), IEEE doubles
    total_shares = sum(c * (p / 100.0) for p, c in zip(PCTS, counts))
    vps = pool / total_shares
    out = []
    for p, c in zip(PCTS, counts):
        if c > 0:
            out.append(math.floor(vps * (p / 100.0)))
    return out

def new(pool, counts):
    shares = sum(c * p for p, c in zip(PCTS, counts))
    return [pool * p // shares for p, c in zip(PCTS, counts) if c > 0]

diffs = []
checked = 0
for counts in itertools.product(range(0, 9), repeat=4):
    if sum(counts) == 0:
        continue
    for pool in range(1, 1501):
        checked += 1
        o, n = old(pool, counts), new(pool, counts)
        if o != n:
            diffs.append((pool, counts, o, n))

print("combinations checked:", checked)
print("differences:", len(diffs))
for d in diffs[:15]:
    print(d)

