"""Regresje geometrii i eksportu rzeźbiarza visual hull."""
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import rzezbiarz as r


def test_mask_gradient_background_and_one_pixel_closing():
    a = np.full((8, 8, 3), 128, np.uint8)
    a[2:6, 2:6] = (60, 100, 40)
    a[3, 3] = (128, 128, 128)
    mask = r.maska(Image.fromarray(a), 25)
    assert mask[3, 3]
    assert mask.sum() == 16


def test_visual_hull_respects_mirrored_back_and_side():
    front = np.zeros((4, 5), bool); side = np.zeros((4, 3), bool); back = np.zeros((4, 5), bool)
    front[1, 0] = True; side[1, 2] = True; back[1, 4] = True
    hull = r.visual_hull(front, side, back)
    assert hull.shape == (4, 5, 3)
    assert hull[1, 0, 2] and hull.sum() == 1


def test_greedy_cuboids_reconstruct_voxels():
    vox = np.zeros((5, 6, 4), bool)
    vox[1:4, 2:5, 1:3] = True
    boxes = r.greedy(vox)
    rebuilt = np.zeros_like(vox)
    for lo, hi in boxes:
        rebuilt[lo[0]:hi[0], lo[1]:hi[1], lo[2]:hi[2]] = True
        assert all(b > a for a, b in zip(lo, hi))
    assert np.array_equal(rebuilt, vox)
    assert len(boxes) == 1


def test_skeleton_explicit_region_overrides_nearest_segment():
    bones = [dict(nazwa='korzen', rodzic=None, pivot=[0, 0, 0]),
             dict(nazwa='glowa', rodzic='korzen', pivot=[1, 3, 1], obszar=[[-1, -1, -1], [2, 2, 2]])]
    labels = r.przypisz_kosci(np.ones((2, 2, 2), bool), bones)
    assert set(labels.flatten()) == {'glowa'}


def test_merge_one_voxel_gap_reduces_box_count():
    boxes = [((0, 0, 0), (1, 2, 1)), ((0, 3, 0), (1, 5, 1))]
    merged = r.scal_z_tolerancja(boxes, 1)
    assert merged == [((0, 0, 0), (1, 5, 1))]


def test_buried_cube_face_is_culled_from_atlas():
    boxes = [((0, 0, 0), (2, 2, 2)), ((0, 2, 0), (2, 4, 2))]
    visible = r.widoczne_sciany(boxes)
    assert 'east' not in visible[0]
    assert 'west' not in visible[1]
    assert 'north' in visible[0]
